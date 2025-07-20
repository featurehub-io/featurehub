Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    Given I create an application strategy tagged "first"
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    And I get the feature history
    When I attach application strategy "first" to the current environment feature value
    And I expect the application strategy "first" to be attached to the feature history
    And the application strategy "first" should be used in 1 environment with 1 feature
    Then the feature flag has an application strategy "first" which has a value of "true"
    Then I create an application strategy tagged "second"
    And I get the feature history
    When I attach application strategy "second" to the current environment feature value
    And I expect the application strategy "second" to be attached to the feature history
    And I save this spot in feature history as "phred" for application strategy "second"
    And there is an application strategy called "first" in the current environment feature value
    And I swap the order of "first" and "second" they remain swapped
    And I get the feature history
    And I delete the application strategy called "second" from the current environment feature value
    And I expect the application strategy "second" to be removed from the feature history
    And I find the feature history and compare it to "phred" and it is the same
#    And I attach application strategy "first" to the current environment feature value
#    And I set the feature flag to off and locked
