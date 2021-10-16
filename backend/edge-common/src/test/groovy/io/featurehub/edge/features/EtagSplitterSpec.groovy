package io.featurehub.edge.features

import io.featurehub.edge.KeyParts
import spock.lang.Specification

class EtagSplitterSpec extends Specification {
  def "when i have tags and a context tag it gets broken up properly"() {
    given: "i have tags"
      def etag = "1;2//other"
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

  def "when i pass in an etag holder and a bunch of feature requests, i get the expected etag header"() {
    given: "an existing etag holder"
      def tag = new EtagStructureHolder([:], "context-key", true)
    when: "i generate the tag"
      def etag = ETagSplitter.@Companion.makeEtags(tag, ["r1", "r2"])
    then:
      etag == 'r1;r2//context-key'
  }
}
