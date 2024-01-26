package io.featurehub.strategies.percentage

import spock.lang.Specification
import spock.lang.Unroll

class PercentageMumurCalculatorSpec extends Specification {
  @Unroll
  def "murmur-hash-comparisons"() {
    when:
    def x = new PercentageMumurCalculator().determineClientPercentage(pKey, featureId)
    then:
    x == result
    where:
    pKey         | featureId               || result
    "fred"       | "abcde"                 || 212628
    "zappo-food" | "172765e02-2-1-1-2-2-1" || 931882
  }
}
