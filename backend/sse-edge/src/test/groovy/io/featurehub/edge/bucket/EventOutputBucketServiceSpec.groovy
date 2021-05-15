package io.featurehub.edge.bucket

import spock.lang.Specification

class EventOutputBucketServiceSpec extends Specification {
  def "a dacha communication time that is longer than the maximum number of slots should fail to create service"() {
    given: "i have set the dacha timeout"
      System.setProperty("edge.dacha.response-timeout", "100000") // 100 seconds
    and: "the max slots is less"
      System.setProperty("maxSlots", "20")
    when: "i create the event output bucket service"
      new EventOutputBucketService()
    then:
        thrown(RuntimeException)
  }

  def timerStarted

  def "when we create the bucket service it starts the timer"() {
    given: "a var"
      timerStarted = false
    when: "created"
      new EventOutputBucketService() {
        @Override
        protected void startTimer() {
          timerStarted = true
        }
      }
   then:
       timerStarted
  }

  def cleanup() {
    System.clearProperty("edge.dacha.response-timeout");
    System.clearProperty("maxSlots")
  }
}
