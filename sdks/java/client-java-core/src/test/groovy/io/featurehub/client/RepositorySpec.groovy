package io.featurehub.client

import com.fasterxml.jackson.databind.ObjectMapper
import io.featurehub.sse.model.FeatureState
import io.featurehub.sse.model.FeatureValueType
import io.featurehub.sse.model.SSEResultState
import io.featurehub.sse.model.StrategyAttributeCountryName
import io.featurehub.sse.model.StrategyAttributeDeviceName
import io.featurehub.sse.model.StrategyAttributePlatformName
import spock.lang.Specification

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService


enum Fruit implements Feature { banana, peach, peach_quantity, peach_config, dragonfruit }

class RepositorySpec extends Specification {
  ClientFeatureRepository repo
  ExecutorService exec

  def setup() {
    exec = [
      execute: { Runnable cmd -> cmd.run() },
      shutdownNow: { -> }
    ] as ExecutorService

    repo = new ClientFeatureRepository(exec)
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
        new FeatureState().id('3').key('peach_quantity').version(1L).value(17).type(FeatureValueType.NUMBER),
        new FeatureState().id('4').key('peach_config').version(1L).value("{}").type(FeatureValueType.JSON),
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
      repo.exists('banana')
      repo.exists(Fruit.banana)
      !repo.exists('dragonfruit')
      !repo.exists(Fruit.dragonfruit)
      repo.getFeatureState('banana').rawJson == null
      repo.getFeatureState('banana').string == null
      repo.getFeatureState('banana').number == null
      repo.getFeatureState('banana').number == null
      repo.getFeatureState('banana').set
      repo.getFeatureState('peach').string == 'orange'
      repo.exists('peach')
      repo.exists(Fruit.peach)
      repo.getFeatureState('peach').key == 'peach'
      repo.getFeatureState('peach').number == null
      repo.getFeatureState('peach').rawJson == null
      repo.getFeatureState('peach').boolean == null
      repo.getFeatureState('peach_quantity').number == 17
      repo.getFeatureState('peach_quantity').rawJson == null
      repo.getFeatureState('peach_quantity').boolean == null
      repo.getFeatureState('peach_quantity').string == null
      repo.getFeatureState('peach_quantity').key == 'peach_quantity'
      repo.getFeatureState('peach_config').rawJson == '{}'
      repo.getFeatureState('peach_config').string == null
      repo.getFeatureState('peach_config').number == null
      repo.getFeatureState('peach_config').boolean == null
      repo.getFeatureState('peach_config').key == 'peach_config'
  }

  def "i can make all features available directly"() {
    given: "we have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
      ]
    when:
      repo.notify(features, false)
      def feature = repo.getFeatureState('banana').copy()
    and: "i make a change to the state but keep the version the same (ok because this is what rollout strategies do)"
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
      feature2.boolean == true
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
      def featureDel = new FeatureState().id('1').key('banana').version(2L).value(true).type(FeatureValueType.BOOLEAN)
      repo.notify(SSEResultState.DELETE_FEATURE, new ObjectMapper().writeValueAsString(featureDel))
    then:
      f.boolean
      !repo.getFeatureState('banana').set
  }


  def "i add an analytics collector and log and event"() {
    given: "i have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
        new FeatureState().id('2').key('peach').version(1L).value("orange").type(FeatureValueType.STRING),
        new FeatureState().id('3').key('peach_quantity').version(1L).value(17).type(FeatureValueType.NUMBER),
        new FeatureState().id('4').key('peach_config').version(1L).value("{}").type(FeatureValueType.JSON),
      ]
    and: "i redefine the executor in the repository so i can prevent the event logging and update first"
      List<Runnable> commands = []
      ExecutorService mockExecutor = [
        execute: { Runnable cmd -> commands.add(cmd) },
        shutdownNow: { -> }
      ] as ExecutorService
      def newRepo = new ClientFeatureRepository(mockExecutor)
      newRepo.notify(features)
      commands.each {it.run() } // process
    and: "i register a mock analyics collector"
      def mockAnalytics = Mock(AnalyticsCollector)
      newRepo.addAnalyticCollector(mockAnalytics)
    when: "i log an event"
      newRepo.logAnalyticsEvent("action", ['a': 'b'])
      def heldNotificationCalls = new ArrayList<Runnable>(commands)
      commands.clear()
    and: "i change the status of the feature"
      newRepo.notify(SSEResultState.FEATURE, new ObjectMapper().writeValueAsString(
        new FeatureState().id('1').key('banana').version(2L).value(true)
          .type(FeatureValueType.BOOLEAN),))
      commands.each {it.run() } // process
      heldNotificationCalls.each {it.run() } // process
    then:
      newRepo.getFeatureState('banana').boolean
      1 * mockAnalytics.logEvent('action', ['a': 'b'], { List<io.featurehub.client.FeatureState> f ->
        f.size() == 4
        f.find({return it.key == 'banana'}) != null
        !f.find({return it.key == 'banana'}).boolean
      })
  }

  def "a json config will properly deserialize into an object"() {
    given: "i have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value('{"sample":12}').type(FeatureValueType.JSON),
      ]
    and: "i register an alternate object mapper"
      repo.setJsonConfigObjectMapper(new ObjectMapper())
    when: "i notify of features"
      repo.notify(features)
    then: 'the json object is there and deserialises'
      repo.getFeatureState('banana').getJson(BananaSample) instanceof BananaSample
      repo.getFeatureState(Fruit.banana).getJson(BananaSample) instanceof BananaSample
      repo.getFeatureState('banana').getJson(BananaSample).sample == 12
      repo.getFeatureState(Fruit.banana).getJson(BananaSample).sample == 12
  }

  def "failure changes readyness to failure"() {
    given: "i have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
      ]
    and: "i notify the repo"
      def mockReadyness = Mock(ReadynessListener)
      repo.addReadynessListener(mockReadyness)
      repo.notify(features)
      def readyness = repo.readyness
    when: "i indicate failure"
      repo.notify(SSEResultState.FAILURE, null)
    then: "we swap to not ready"
      repo.readyness == Readyness.Failed
      readyness == Readyness.Ready
      1 * mockReadyness.notify(Readyness.Failed)
  }

  def "ack and bye are ignored"() {
    given: "i have features"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
      ]
    and: "i notify the repo"
      repo.notify(features)
    when: "i ack and then bye, nothing happens"
      repo.notify(SSEResultState.ACK, null)
      repo.notify(SSEResultState.BYE, null)
    then:
      repo.readyness == Readyness.Ready
  }

  def "i can attach to a feature before it is added and receive notifications when it is"() {
    given: "i have one of each feature type"
      def features = [
        new FeatureState().id('1').key('banana').version(1L).value(false).type(FeatureValueType.BOOLEAN),
        new FeatureState().id('2').key('peach').version(1L).value("orange").type(FeatureValueType.STRING),
        new FeatureState().id('3').key('peach_quantity').version(1L).value(17).type(FeatureValueType.NUMBER),
        new FeatureState().id('4').key('peach_config').version(1L).value("{}").type(FeatureValueType.JSON),
      ]
    and: "I listen for updates for those features"
      def updateListener = []
      List<io.featurehub.client.FeatureState> emptyFeatures = []
      features.each {f ->
        def feature = repo.getFeatureState(f.key)
        def listener = Mock(FeatureListener)
        updateListener.add(listener)
        feature.addListener(listener)
        emptyFeatures.add(feature.copy())
      }
    when: "i fill in the repo"
      repo.notify(features)
    then:
      updateListener.each {
        1 * it.notify(_)
      }
      emptyFeatures.each {f ->
        f.key != null
        !f.set
        f.string == null
        f.boolean == null
        f.rawJson == null
        f.number == null
      }
    features.each { it ->
      repo.getFeatureState(it.key).key == it.key
      repo.getFeatureState(it.key).set

      if (it.type == FeatureValueType.BOOLEAN)
        repo.getFeatureState(it.key).boolean == it.value
      else
        repo.getFeatureState(it.key).boolean == null

      if (it.type == FeatureValueType.NUMBER)
        repo.getFeatureState(it.key).number == it.value
      else
        repo.getFeatureState(it.key).number == null

      if (it.type == FeatureValueType.STRING)
        repo.getFeatureState(it.key).string.equals(it.value)
      else
        repo.getFeatureState(it.key).string == null

      if (it.type == FeatureValueType.JSON)
        repo.getFeatureState(it.key).rawJson.equals(it.value)
      else
        repo.getFeatureState(it.key).rawJson == null
    }

  }

  def "the client context encodes as expected"() {
    when: "i encode the context"
      def tc = new TestContext().userKey("DJElif")
        .country(StrategyAttributeCountryName.TURKEY)
        .attr("city", "Istanbul")
        .attrs("musical styles", Arrays.asList("psychedelic", "deep"))
        .device(StrategyAttributeDeviceName.DESKTOP)
        .platform(StrategyAttributePlatformName.ANDROID)
        .version("2.3.7")
        .sessionKey("anjunadeep").build().get()

    and: "i do the same thing again to ensure i can reset everything"
      tc.userKey("DJElif")
        .country(StrategyAttributeCountryName.TURKEY)
        .attr("city", "Istanbul")
        .attrs("musical styles", Arrays.asList("psychedelic", "deep"))
        .device(StrategyAttributeDeviceName.DESKTOP)
        .platform(StrategyAttributePlatformName.ANDROID)
        .version("2.3.7")
        .sessionKey("anjunadeep").build().get()
    then:
      FeatureStateUtils.generateXFeatureHubHeaderFromMap(tc.context()) ==
        'city=Istanbul,country=turkey,device=desktop,musical styles=psychedelic%2Cdeep,platform=android,session=anjunadeep,userkey=DJElif,version=2.3.7'
  }
}
