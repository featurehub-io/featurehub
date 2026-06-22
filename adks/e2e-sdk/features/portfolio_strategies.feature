@allvariants @streamingvariants
Feature: We can save, change and retrieve portfolio strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @appstrat
  Scenario: I can create a feature value with an portfolio strategy attached
    Given I create a new normal group
    And I assign portfolio strategy "edit,delete" roles to the group
    And I create a new user
    And I assign the new user to the new group
    Given I create an portfolio strategy tagged "first"
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    And I get the feature history
    When I attach portfolio strategy "first" to the current environment feature value
    And I expect the portfolio strategy "first" to be attached to the feature history
    And the portfolio strategy "first" should be used in 1 environment with 1 feature
    Then the feature flag has an portfolio strategy "first" which has a value of "true"
    Then I create an portfolio strategy tagged "second"
    And I get the feature history
    When I attach portfolio strategy "second" to the current environment feature value
    And I expect the portfolio strategy "second" to be attached to the feature history
    And I save this spot in feature history as "phred" for portfolio strategy "second"
    And there is an portfolio strategy called "first" in the current environment feature value
    And I swap the order of "first" and "second" they remain swapped
    And I get the feature history
    And I delete the portfolio strategy called "second" from the current environment feature value
    And I expect the portfolio strategy "second" to be removed from the feature history
    And I find the feature history and compare it to "phred" and it is the same
#    And I attach portfolio strategy "first" to the current environment feature value
#    And I set the feature flag to off and locked
