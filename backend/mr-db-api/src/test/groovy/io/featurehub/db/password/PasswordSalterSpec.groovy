package io.featurehub.db.password

import spock.lang.Specification


class PasswordSalterSpec extends Specification {
  private final String ALG = 'PBKDF2WithHmacSHA512'

  def "i can validated a salted password"() {
    given: "i have a salted password"
      String original = "Muppet\$%^actor"
    and: "I have a salter"
      PasswordSalter salter = new PasswordSalter()
    when: "i salt it"
      String salted = salter.saltPassword(original, ALG).get()
    then: "it validates"
      salter.validatePassword(original, salted, ALG)
  }

  def "i cannot salt a null password"() {
    when: "i salt a null password"
      String val = null;
      new PasswordSalter().saltPassword(null, ALG).ifPresent({v -> val = v})
    then:
      val == null
  }

  def "i cannot salt an empty password"() {
    when: "i salt an empty password"
      String val = null;
      new PasswordSalter().saltPassword("", ALG).ifPresent({v -> val = v})
    then:
      val == null
  }
}
