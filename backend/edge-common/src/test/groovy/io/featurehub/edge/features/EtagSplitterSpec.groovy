package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import spock.lang.Specification

class EtagSplitterSpec extends Specification {
  def "when i have tags and a context tag it gets broken up properly"() {
    given: "i have tags"
      def etag = '"1;2//other"'
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      holder.validEtag
      holder.contextTag == "other"
      holder.environmentTags[k1] == "1"
      holder.environmentTags[k2] == "2"
  }

  def "a weak etag (from a compressed response) is split the same as a strong one"() {
    given: "i have a weak etag"
      def etag = 'W/"1;2//other"'
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      holder.validEtag
      holder.contextTag == "other"
      holder.environmentTags[k1] == "1"
      holder.environmentTags[k2] == "2"
  }

  def "when the etag is null, the holder will be invalid but the context etag will still be the context's one"() {
    given: "i have tags"
      def etag = null
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      !holder.validEtag
      holder.contextTag == "other"
  }

  def "when the keys have less in the header than are passed, the holder is invalid"() {
    given: "i have tags"
      def etag = "1;//other"
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      !holder.validEtag
      holder.contextTag == "other"
  }

  def "when the keys have more in the header than are passed, the holder is invalid"() {
    given: "i have tags"
      def etag = "1;2//other"
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def keypairs = [k1]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      !holder.validEtag
      holder.contextTag == "other"
  }

  def "when the keys are right but the context match is wrong"() {
    given: "i have tags"
      def etag = "1;2//other2"
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      !holder.validEtag
      holder.contextTag == "other"
  }

  def "when the keys are right but there is no context"() {
    given: "i have tags"
      def etag = "1;2"
    and: "i have two keypairs"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
      def k2 = new KeyParts("1", UUID.randomUUID(), "s2")
      def keypairs = [k1,k2]
    when: "i split"
      def holder = ETagSplitter.@Companion.splitTag(etag, keypairs, "other")
    then:
      !holder.validEtag
      holder.contextTag == "other"
  }

  def "sse: a last-event-id is evaluated against the client context"() {
    given: "a single sse key"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
    when: "i make the sse etag holder"
      def holder = ETagSplitter.@Companion.parseSseEtag(lastEventId, [k1], clientContext)
    then:
      holder.validEtag == valid
      holder.contextTag == clientContext
      holder.environmentTags[k1] == (valid ? "abc" : null)
    where:
      lastEventId       | clientContext || valid
      "abc"             | "0"           || true    // no context then, no context now
      "abc//ctx1"       | "ctx1"        || true    // same context
      '"abc//ctx1"'     | "ctx1"        || true    // tolerate a quoted id
      "abc"             | "ctx1"        || false   // context added since the id was issued
      "abc//ctx1"       | "0"           || false   // context removed since the id was issued
      "abc//ctx1"       | "ctx2"        || false   // context changed
      ""                | "0"           || false
      '""'              | "0"           || false
      null              | "0"           || false
  }

  def "sse: the event id we issue is accepted back as a valid last-event-id for the same context"() {
    given: "a single sse key"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
    and: "the id we would send on the features event"
      def id = ETagSplitter.@Companion.makeEtags(new EtagStructureHolder([:], ctx, false), ["dacha-etag"])
    when: "the client reconnects with it"
      def holder = ETagSplitter.@Companion.parseSseEtag(id, [k1], ctx)
    then:
      holder.validEtag
      holder.environmentTags[k1] == "dacha-etag"
    where:
      ctx << ["0", "1a2b3c"]
  }

  def "sse: when etags for sse are disabled (the default), a matching last-event-id is ignored"() {
    given: "a single sse key"
      def k1 = new KeyParts("1", UUID.randomUUID(), "s1")
    when: "i make the sse etag holder with a valid id"
      def holder = ETagSplitter.@Companion.makeEtagHolderForSSE("abc", [k1], "0")
    then:
      ETagSplitter.@Companion.etagsEnabledForSse || !holder.validEtag
  }

  def "when i pass in an etag holder and a bunch of feature requests, i get the expected etag header"() {
    given: "an existing etag holder"
      def tag = new EtagStructureHolder([:], "context-key", true)
    when: "i generate the tag"
      def etag = ETagSplitter.@Companion.makeEtags(tag, ["r1", "r2"])
    then:
      etag == 'r1;r2//context-key'
  }
}
