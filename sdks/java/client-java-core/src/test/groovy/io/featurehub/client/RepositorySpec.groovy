package io.featurehub.client

import com.fasterxml.jackson.databind.ObjectMapper
import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import io.featurehub.sse.model.SSEResultState
import spock.lang.Specification

import java.util.concurrent.Executor

class RepositorySpec extends Specification {
  FeatureRepository repo

  def setup() {
    repo = new ClientFeatureRepository(1) {
      @Override
      protected Executor getExecutor(int threadPoolSize) {
        return new Executor() {
          @Override
          void execute(Runnable command) {
            // make it synchronous
            command.run()
          }
        }
      }
    }
  }

  def "an empty repository is not ready"() {
    when: "ask for the readyness status"
      def ready = repo.readyness
    then:
      ready == Readyness.NotReady
  }

  def "a set of features should trigger readyness and make all features available"() {
    given: "we have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
        new FeatureState().id('2').key('peach').version(1L).value("orange").type(FeatureValueType.STRING),
        new FeatureState().id('3').key('peach-quantity').version(1L).value(17).type(FeatureValueType.NUMBER),
        new FeatureState().id('4').key('peach-config').version(1L).value("{}").type(FeatureValueType.JSON),
      ]
    and: "we have a readyness listener"
      def readynessListener = Mock(ReadynessListener)
      repo.addReadynessListener(readynessListener)
    when:
      repo.notify(SSEResultState.FEATURES, new ObjectMapper().writeValueAsString(features))
    then:
      1 * readynessListener.notify(Readyness.Ready)
      !repo.getFeatureState('banana').boolean
      repo.getFeatureState('banana').key == 'banana'
      repo.getFeatureState('banana').rawJson == null
      repo.getFeatureState('banana').string == null
      repo.getFeatureState('banana').number == null
      repo.getFeatureState('banana').number == null
      repo.getFeatureState('banana').set
      repo.getFeatureState('peach').string == 'orange'
      repo.getFeatureState('peach').key == 'peach'
      repo.getFeatureState('peach').number == null
      repo.getFeatureState('peach').rawJson == null
      repo.getFeatureState('peach').boolean == null
      repo.getFeatureState('peach-quantity').number == 17
      repo.getFeatureState('peach-quantity').rawJson == null
      repo.getFeatureState('peach-quantity').boolean == null
      repo.getFeatureState('peach-quantity').string == null
      repo.getFeatureState('peach-quantity').key == 'peach-quantity'
      repo.getFeatureState('peach-config').rawJson == '{}'
      repo.getFeatureState('peach-config').string == null
      repo.getFeatureState('peach-config').number == null
      repo.getFeatureState('peach-config').boolean == null
      repo.getFeatureState('peach-config').key == 'peach-config'
  }

  def "i can make all features available directly"() {
    given: "we have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
      ]
    when:
      repo.notify(features, false)
      def feature = repo.getFeatureState('banana').copy()
    and: "i make a change to the state but keep the version the same"
      repo.notify([
        new FeatureState().id('1').key('banana').version(1L).value(true).type(FeatureValueType.BOOLEAN),
      ])
      def feature2 = repo.getFeatureState('banana').copy()
    and: "then i make the change but up the version"
      repo.notify([
        new FeatureState().id('1').key('banana').version(2L).value(true).type(FeatureValueType.BOOLEAN),
      ])
      def feature3 = repo.getFeatureState('banana').copy()
    and: "then i make a change but force it even if the version is the same"
      repo.notify([
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
      ], true)
      def feature4 = repo.getFeatureState('banana').copy()
    then:
      feature.boolean == false
      feature2.boolean == false
      feature3.boolean == true
      feature4.boolean == false
  }

  def "a non existent feature is not set"() {
    when: "we ask for a feature that doesn't exist"
      def feature = repo.getFeatureState('fred')
    then:
      !feature.set
  }

  def "a feature is deleted that doesn't exist and thats ok"() {
    when: "i create a feature to delete"
      def feature = new FeatureState().id('1').key('banana').version(1L).value(true).type(FeatureValueType.BOOLEAN)
    and: "i delete a non existent feature"
      repo.notify(SSEResultState.DELETE_FEATURE, new ObjectMapper().writeValueAsString(feature))
    then:
      !repo.getFeatureState('banana').set
  }

  def "A feature is deleted and it is now not set"() {
    given: "i have a feature"
      def feature = new FeatureState().id('1').key('banana').version(1L).value(true).type(FeatureValueType.BOOLEAN)
    and: "i notify repo"
      repo.notify([feature])
    when: "i check the feature state"
      def f = repo.getFeatureState('banana').copy()
    and: "i delete the feature"
      repo.notify(SSEResultState.DELETE_FEATURE, new ObjectMapper().writeValueAsString(feature))
    then:
      f.b
      !repo.getFeatureState('banana').set

  }

  def "i add an analytics collector and log and event"() {

  }

  def "failure changes readyness to failure"() {

  }

  def "ack and bye are ignored"() {

  }
}
