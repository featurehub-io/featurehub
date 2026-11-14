package io.featurehub.dacha2.client

import io.featurehub.dacha2.Dacha2CacheImpl
import io.featurehub.dacha2.Dacha2NewCacheImpl
import io.featurehub.dacha2.FeatureValuesFactoryImpl
import io.featurehub.dacha2.api.Dacha2ServiceClient
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.ServerErrorException
import kotlin.jvm.functions.Function0
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList

class MRUnavailableLoggingSpec extends Specification {
  static class Capture extends AbstractAppender {
    final List<LogEvent> events = new CopyOnWriteArrayList<>()

    Capture() {
      super("mr-unavailable-capture", null, null, true, Property.EMPTY_ARRAY)
      start()
    }

    @Override
    void append(LogEvent event) {
      events.add(event.toImmutable())
    }

    List<LogEvent> at(Level level) { events.findAll { it.level == level } }
  }

  Capture capture
  Dacha2ServiceClient mr
  long now

  def setup() {
    MRUnavailable.INSTANCE.reset()
    now = 1_000_000
    MRUnavailable.INSTANCE.clock = { -> now } as Function0<Long>
    capture = new Capture()
    def ctx = (LoggerContext) LogManager.getContext(false)
    ctx.configuration.getLoggerConfig("io.featurehub.dacha2").addAppender(capture, Level.ALL, null)
    ctx.updateLoggers()
    mr = Mock()
  }

  def cleanup() {
    def ctx = (LoggerContext) LogManager.getContext(false)
    ctx.configuration.getLoggerConfig("io.featurehub.dacha2").removeAppender(capture.name)
    ctx.updateLoggers()
    MRUnavailable.INSTANCE.reset()
  }

  def cache(String impl) {
    return impl == "guava" ? new Dacha2CacheImpl(mr, new FeatureValuesFactoryImpl())
      : new Dacha2NewCacheImpl(mr, new FeatureValuesFactoryImpl())
  }

  void quietly(Closure c) {
    try { c() } catch (Throwable ignored) {}
  }

  def "an MR outage (#impl cache) logs no errors, counts each failed load and logs one WARN summary"() {
    given:
      def c = cache(impl)
      def envBefore = MRUnavailable.INSTANCE.unavailableCount("environment")
      def saBefore = MRUnavailable.INSTANCE.unavailableCount("service-account")
      def failures = [new MRUnavailableException("environment is not in redis"), new ProcessingException("connection refused"),
                      new ServerErrorException(503), new ServerErrorException(502), new ServerErrorException(504)]
      def next = 0
      mr.getEnvironment(_, _) >> { throw failures[next++ % failures.size()] }
      mr.getServiceAccount(_, _) >> { throw new MRUnavailableException("service account key is not in redis") }
    when:
      failures.size().times { quietly { c.findEnvironment(UUID.randomUUID()) } }
      3.times { quietly { c.getFeatureCollection(UUID.randomUUID(), "key-$it", false) } }
    then:
      new PollingConditions(timeout: 5).eventually {
        assert capture.at(Level.WARN).size() == 1
      }
      capture.at(Level.ERROR).isEmpty()
      capture.at(Level.WARN)[0].message.formattedMessage.contains("because MR is unavailable")
      capture.at(Level.WARN)[0].thrown == null
      MRUnavailable.INSTANCE.unavailableCount("environment") - envBefore == failures.size()
      MRUnavailable.INSTANCE.unavailableCount("service-account") - saBefore == 3
    cleanup:
      c.closeCache()
    where:
      impl << ["guava", "caffeine"]
  }

  def "a #status from MR (#impl cache) is not an outage, it is logged at ERROR with its stack trace"() {
    given:
      def c = cache(impl)
      mr.getEnvironment(_, _) >> { throw new ServerErrorException(status) }
    when:
      quietly { c.findEnvironment(UUID.randomUUID()) }
    then:
      new PollingConditions(timeout: 5).eventually {
        assert capture.at(Level.ERROR).size() == 1
      }
      capture.at(Level.ERROR)[0].thrown != null
      capture.at(Level.WARN).isEmpty()
    cleanup:
      c.closeCache()
    where:
      impl       | status
      "guava"    | 500
      "caffeine" | 500
      "caffeine" | 501
  }

  def "while the outage continues there is one WARN summary per interval, with the count since the last one"() {
    given:
      def c = cache("caffeine")
      mr.getEnvironment(_, _) >> { throw new ProcessingException("connection refused") }
    when: "the first failure is summarised straight away, the next ones within the minute are not"
      4.times { quietly { c.findEnvironment(UUID.randomUUID()) } }
      now += 60_000
      quietly { c.findEnvironment(UUID.randomUUID()) }
    then:
      new PollingConditions(timeout: 5).eventually {
        assert capture.at(Level.WARN).size() == 2
      }
      capture.at(Level.WARN)[0].message.formattedMessage.startsWith("dacha2: 1 cache loads failed because")
      capture.at(Level.WARN)[1].message.formattedMessage.startsWith("dacha2: 4 cache loads failed in the last 60s because")
      capture.at(Level.ERROR).isEmpty()
    cleanup:
      c.closeCache()
  }

  def "the fallback client's own failures are MRUnavailableExceptions, so the loaders treat them as an outage"() {
    expect:
      MRUnavailable.INSTANCE.isExpectedDuringOutage(new MRUnavailableException("x"))
      MRUnavailable.INSTANCE.isExpectedDuringOutage(new ProcessingException("x"))
      [502, 503, 504].every { MRUnavailable.INSTANCE.isExpectedDuringOutage(new ServerErrorException(it)) }
      !MRUnavailable.INSTANCE.isExpectedDuringOutage(new ServerErrorException(500))
      !MRUnavailable.INSTANCE.isExpectedDuringOutage(new RuntimeException("bug"))
  }
}
