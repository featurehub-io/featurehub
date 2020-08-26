package io.featurehub.edge.strategies

import io.featurehub.strategies.percentage.PercentageCalculator
import io.featurehub.strategies.percentage.PercentageMumurCalculator
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class PercentageFeatureSpec extends Specification {
  PercentageCalculator calc

  def setup() {
    calc = new PercentageMumurCalculator(0)
  }

  def "for 1000 random keys we will get about an 20% being in our 'bucket'"() {
    given: "a feature id"
      def featureId = UUID.randomUUID().toString()
    when: "we loop through 1000 random strings"
      def counter = 0
      for(int count = 0; count < 1000; count++) {
        if (calc.determineClientPercentage(RandomStringUtils.randomAlphanumeric(6), featureId) <= 200000) {
          counter ++
        }
      }
      print "counter is $counter"
    then: "counter should be around 200"
      counter >= 160
      counter <= 240

  }
}
