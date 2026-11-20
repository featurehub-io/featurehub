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

  def "the file existing without a blocked key does not block"() {
    given:
      def f = dir.resolve("update-block").toFile()
      f.text = content
    expect:
      block(f.absolutePath).blockedRetryAfter() == null
    where:
      content << ["", "\n\n", "# blocked\n", "ling=orm\nkeep-silent: true\n", "blockedx=1", "Blocked=1"]
  }

  def "a missing file does not block"() {
    expect:
      block(dir.resolve("not-there").toFile().absolutePath).blockedRetryAfter() == null
  }

  def "the blocked key blocks, and removing the key (leaving the file) unblocks"() {
    given:
      def f = dir.resolve("update-block").toFile()
      f.text = "ormkorn=always wonder\nblocked\n"
    when:
      def b = block(f.absolutePath, 3600, "120")
    then: "it is checked straight away at startup, an empty value uses the default retry-after"
      b.blockedRetryAfter() == "120"
    when:
      f.text = "ormkorn=always wonder\n"
      b.check()
    then:
      f.exists()
      b.blockedRetryAfter() == null
    when: "and adding it back blocks again"
      f.text = "blocked=\n"
      b.check()
    then:
      b.blockedRetryAfter() == "120"
  }

  def "the blocked value is the retry-after when it is seconds or an http date, otherwise the default"() {
    given:
      def f = dir.resolve("update-block").toFile()
      f.text = content
    expect:
      block(f.absolutePath, 3600, "60").blockedRetryAfter() == expected
    where:
      content                                     | expected
      "blocked=300"                               | "300"
      "blocked: 300"                              | "300"
      "  blocked = 45  "                          | "45"
      "blocked: Fri, 13 Nov 2026 10:00:00 GMT"    | "Fri, 13 Nov 2026 10:00:00 GMT"
      "blocked=true"                              | "60"
      "blocked=-5"                                | "60"
      "blocked=kwong pastry"                      | "60"
  }

  def "a directory at the path does not block"() {
    given:
      def b = block(dir.toFile().absolutePath)
    expect:
      b.blockedRetryAfter() == null
  }

  def "an unreadable file does not block"() {
    given:
      def f = dir.resolve("update-block").toFile()
      f.text = "blocked=30"
      def b = block(f.absolutePath)
    expect:
      b.blockedRetryAfter() == "30"
    when:
      f.setReadable(false)
      b.check()
    then:
      f.canRead() || b.blockedRetryAfter() == null  // root can always read, so only assert when it really is unreadable
    cleanup:
      f.setReadable(true)
  }

  def "the scheduled check keeps polling, picking up the key being added and removed"() {
    given:
      def f = dir.resolve("update-block").toFile()
      f.text = "lingling=kwong"
      def b = block(f.absolutePath, 1)
    when:
      f.text = "lingling=kwong\nblocked=30"
      waitFor { b.blockedRetryAfter() != null }
    then:
      b.blockedRetryAfter() == "30"
    when:
      f.text = "lingling=kwong"
      waitFor { b.blockedRetryAfter() == null }
    then:
      b.blockedRetryAfter() == null
  }

  private static void waitFor(Closure<Boolean> condition) {
    def deadline = System.currentTimeMillis() + 5000
    while (!condition() && System.currentTimeMillis() < deadline) {
      Thread.sleep(50)
    }
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
