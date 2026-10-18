package io.featurehub.edge


import spock.lang.Specification

// the purpose of this class and these tests are to be run manually and to ensure that the SSE provider of each SDK
// is honouring the last-event-id as expected with the new setup. It operates on the assumption that it will be run against a todo
// application that runs on port TODO_PORT (8099), it will do a health check and once it gets a 200 it will ensure the uppercase
// feature is on (by asking the REST endpoint), save a new todo
// get the todo and ensure it is upper case. This ensures that the todo server is receiving the repository state. It will then
// change the uppercase to false, confirm it is false and then get the todo it sent before and ensure it is now lower case.
//  It will give it a time interval in which this is allowed to occur in.
// it will read the test environment from /adk/e2e/test-example.env
class SSELastEventIdRunner extends Specification {


  def "the backend server should operate using sse as expected"() {
    given: "i read the test environment information"
      def utils = new TodoUtils()
      def env = utils.readTestEnv()
    and: "i setup the edge server"
      utils.setupEdgeService(env)
    and: "i choose my flag"
      def key = "FEATURE_TITLE_TO_UPPERCASE"
    when: "i set the feature flag to false"
      def flagValue = false
      utils.updateFeatureValue(env, key, flagValue)
//    then: "we have some level of success"
//      (200..299).contains(result.statusCode())
    and: "the edge call directly tells me within 30s that it has been updated"
      utils.waitForFeatureValue(env, key, flagValue)
    and: "i wait for the todo server to become live and have repository state"
      utils.todoLivenessCheck()
      Thread.sleep(1000)
    then:
      1 == 1
    when: "we post a new todo"
      def brand = "keep silent"
      def todoResult = utils.postTodo("1", brand)
    then:
      todoResult.find({ it.id == "1" }).title == brand.toLowerCase()
    when: "i set the feature flag to true"
      flagValue = true
      utils.updateFeatureValue(env, key, flagValue)
    and: "the edge call directly tells me within 30s that it has been updated"
      utils.waitForFeatureValue(env, key, flagValue)
      Thread.sleep(1000)
    then: "the same todo will be upper case"
      utils.getTodos().find({ it.id == "1" }).title == brand.toUpperCase()
  }
}
