Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    Given I create an application strategy tagged "first"
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    When I attach application strategy "first" to the current environment feature value
    And the application strategy "first" should be used in 1 environment with 1 feature
    Then I create an application strategy tagged "second"
    When I attach application strategy "second" to the current environment feature value
    And there is an application strategy called "first" in the current environment feature value
    And I swap the order of "first" and "second" they remain swapped
    And I delete the application strategy called "second" from the current environment feature value
#    And I attach application strategy "first" to the current environment feature value
#    And I set the feature flag to off and locked
