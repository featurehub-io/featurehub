package io.featurehub.edge.rest

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.utils.UpdateMapper
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.IterableProvider
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class FeatureUpdateBlockSpec extends Specification {
  @TempDir
  Path dir

  List<FileFeatureUpdateBlock> blocks = []

  def cleanup() {
    blocks.each { it.close() }
  }

  FileFeatureUpdateBlock block(String file, long interval = 3600, String retryAfter = "60") {
    def b = new FileFeatureUpdateBlock(file, interval, retryAfter)
    blocks.add(b)
    return b
  }

  def "with no file configured updates are never blocked"() {
    expect:
      block(null).blockedRetryAfter() == null
  }

  def "an existing empty file blocks with the default retry-after, and removing it unblocks"() {
    given:
      def f = dir.resolve("blocked").toFile()
      f.text = ""
    when:
      def b = block(f.absolutePath, 3600, "120")
    then: "it is checked straight away at startup"
      b.blockedRetryAfter() == "120"
    when:
      f.delete()
      b.check()
    then:
      b.blockedRetryAfter() == null
  }

  def "the file's first non-blank line is the retry-after"() {
    given:
      def f = dir.resolve("blocked").toFile()
      def b = block(f.absolutePath)
    expect: "not there yet"
      b.blockedRetryAfter() == null
    when:
      f.text = "\n  Fri, 13 Nov 2026 10:00:00 GMT  \nignored"
      b.check()
    then:
      b.blockedRetryAfter() == "Fri, 13 Nov 2026 10:00:00 GMT"
  }

  def "a directory at the path does not block"() {
    given:
      def b = block(dir.toFile().absolutePath)
    expect:
      b.blockedRetryAfter() == null
  }

  def "the scheduled check picks up the file appearing"() {
    given:
      def f = dir.resolve("blocked").toFile()
      def b = block(f.absolutePath, 1)
    when:
      f.text = "30"
      def deadline = System.currentTimeMillis() + 5000
      while (b.blockedRetryAfter() == null && System.currentTimeMillis() < deadline) {
        Thread.sleep(50)
      }
    then:
      b.blockedRetryAfter() == "30"
  }

  def "a check interval below the minimum uses the minimum"() {
    expect:
      block(dir.resolve("x").toFile().absolutePath, interval).checkIntervalSeconds == FileFeatureUpdateBlock.MINIMUM_CHECK_INTERVAL_SECONDS
    where:
      interval << [0, -5]
  }

  def "while blocked the processor returns 503 with retry-after and never asks dacha or publishes"() {
    given:
      def registry = Mock(DachaClientServiceRegistry)
      def publisher = Mock(FeatureUpdatePublisher)
      def blocker = Stub(FeatureUpdateBlock) { blockedRetryAfter() >> "45" }
      def processor = new FeatureUpdateProcessor(Mock(UpdateMapper), registry, publisher,
        Stub(IterableProvider), blocker)
      def response = Mock(AsyncResponse)
      Response resumed = null
    when:
      processor.updateFeature(response, null, UUID.randomUUID(), "key", "FEATURE",
        new FeatureStateUpdate().lock(true), null)
    then:
      1 * response.resume(_ as Response) >> { args -> resumed = args[0]; true }
      0 * registry._
      0 * publisher._
      resumed.status == 503
      resumed.getHeaderString("Retry-After") == "45"
  }
}
