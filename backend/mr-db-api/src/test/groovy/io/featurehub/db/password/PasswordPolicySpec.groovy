package io.featurehub.db.password

import spock.lang.Specification
import spock.lang.Unroll

class PasswordPolicySpec extends Specification {
  /**
   * min 8, max 40, upper + lower + numeric required, symbol not required.
   */
  private static PasswordPolicy defaultPolicy() {
    return new PasswordPolicy(8, 40, true, true, true, false)
  }

  private static List<PasswordPolicyRule> violationsOf(PasswordPolicy policy, String password) {
    try {
      policy.validate(password)
      return []
    } catch (PasswordPolicyViolationException e) {
      return e.violations
    }
  }

  @Unroll
  def "the default policy accepts '#password'"() {
    when: "i validate a compliant password"
      defaultPolicy().validate(password)
    then: "no violation is raised"
      notThrown(PasswordPolicyViolationException)
    where:
      password << [
        'Passw0rdX',      // plain ascii
        'Ünterwegs9',     // non-ascii upper and lower still count toward their class
        'Passw0rd!',      // a symbol is allowed even though it is not required
        'aB3' + ('x' * 37) // exactly 40 characters
      ]
  }

  def "every violated rule is reported, not just the first"() {
    when: "i validate a password that breaks three rules at once"
      def violations = violationsOf(defaultPolicy(), 'abc')
    then: "all three are reported"
      violations.containsAll([
        PasswordPolicyRule.MIN_LENGTH,
        PasswordPolicyRule.REQUIRE_UPPERCASE,
        PasswordPolicyRule.REQUIRE_NUMERIC
      ])
    and: "the rule it does satisfy is not reported"
      !violations.contains(PasswordPolicyRule.REQUIRE_LOWERCASE)
  }

  def "the reported thresholds are the configured ones"() {
    when: "i validate a too-short password"
      defaultPolicy().validate('aB1')
    then: "the exception carries the configured bounds"
      def e = thrown(PasswordPolicyViolationException)
      e.minLength == 8
      e.maxLength == 40
  }

  def "an operator can relax the policy to accept a simple password"() {
    given: "a policy with a low minimum and no character class requirements"
      def policy = new PasswordPolicy(6, 40, false, false, false, false)
    when:
      policy.validate('simple')
    then:
      notThrown(PasswordPolicyViolationException)
  }

  def "an operator can require a symbol"() {
    given: "a policy that requires a symbol"
      def policy = new PasswordPolicy(8, 40, true, true, true, true)
    expect: "a password without one is rejected for exactly that rule"
      violationsOf(policy, 'Passw0rdX') == [PasswordPolicyRule.REQUIRE_SYMBOL]
    and: "a password with one is accepted"
      violationsOf(policy, 'Passw0rd!').isEmpty()
  }

  def "interior whitespace satisfies the symbol requirement"() {
    given: "a policy that requires a symbol"
      def policy = new PasswordPolicy(8, 40, true, true, true, true)
    when: "the only non-alphanumeric character is a space inside the password"
      policy.validate('Passw0rd X')
    then:
      notThrown(PasswordPolicyViolationException)
  }

  def "the password is validated as trimmed, because that is what gets stored"() {
    expect: "padding whitespace does not count toward the minimum length"
      violationsOf(defaultPolicy(), '   aB1   ').contains(PasswordPolicyRule.MIN_LENGTH)
    and: "a whitespace only password is too short"
      violationsOf(defaultPolicy(), '        ').contains(PasswordPolicyRule.MIN_LENGTH)
    and: "so is a null one"
      violationsOf(defaultPolicy(), null).contains(PasswordPolicyRule.MIN_LENGTH)
  }

  def "a password longer than the maximum is rejected by the policy"() {
    when: "i validate a 41 character otherwise compliant password"
      def password = 'aB3' + ('x' * 38)
      def violations = violationsOf(defaultPolicy(), password)
    then:
      password.length() == 41
      violations == [PasswordPolicyRule.MAX_LENGTH]
  }

  def "an operator can lower the maximum"() {
    given: "a policy with a maximum of 20"
      def policy = new PasswordPolicy(8, 20, true, true, true, false)
    when: "i validate a 25 character password"
      def password = 'aB3' + ('x' * 22)
      policy.validate(password)
    then:
      password.length() == 25
      def e = thrown(PasswordPolicyViolationException)
      e.violations == [PasswordPolicyRule.MAX_LENGTH]
      e.maxLength == 20
  }

  def "a maximum above the transport cap is clamped rather than silently unreachable"() {
    when: "an operator configures a maximum longer than the api schema allows"
      def policy = new PasswordPolicy(8, 200, true, true, true, false)
    then: "it is clamped to the transport cap"
      policy.maxLength == PasswordPolicy.TRANSPORT_MAX_LENGTH
    and: "the other settings are untouched"
      policy.minLength == 8
      policy.requireUppercase
      !policy.requireSymbol
  }

  def "a minimum above the maximum falls back to the whole default policy"() {
    when: "an operator configures a self contradictory policy"
      def policy = new PasswordPolicy(30, 20, false, false, false, true)
    then: "every setting reverts to the default, not just the lengths"
      policy.minLength == PasswordPolicy.DEFAULT_MIN_LENGTH
      policy.maxLength == PasswordPolicy.DEFAULT_MAX_LENGTH
      policy.requireUppercase == PasswordPolicy.DEFAULT_REQUIRE_UPPERCASE
      policy.requireLowercase == PasswordPolicy.DEFAULT_REQUIRE_LOWERCASE
      policy.requireNumeric == PasswordPolicy.DEFAULT_REQUIRE_NUMERIC
      policy.requireSymbol == PasswordPolicy.DEFAULT_REQUIRE_SYMBOL
    and: "a default compliant password is accepted, i.e. the deployment is not bricked"
      violationsOf(policy, 'Passw0rdX').isEmpty()
  }

  def "the violation never leaks the password"() {
    given: "a distinctive password that breaks several rules"
      def password = 'zebra'
    when:
      defaultPolicy().validate(password)
    then:
      def e = thrown(PasswordPolicyViolationException)
    and: "neither the message nor the rules mention any part of it"
      !e.message.contains(password)
      !e.message.toLowerCase().contains('zeb')
      !e.violations.toString().contains(password)
  }
}
