@allvariants @streamingvariants
Feature: We can save, change and retrieve portfolio strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @portstrat
  Scenario: I can create a feature value with an portfolio strategy attached
    Given I create a new normal group
    And I assign portfolio strategy "edit,delete" roles to the group
    And I create a new user
    And I assign the new user to the new group
    Given I create portfolio strategies
      | name   | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | first  | _          | _                    | customer  | equals      | brand  | string |
      | second | _          | _                    | customer  | equals      | brand  | string |
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    And I get the feature history
    When I attach portfolio strategy "first" to the current environment feature value with the value "true" with percentage 15
    And I expect the portfolio strategy "first" to be attached to the feature history with the value set to "true"
    And the portfolio strategy "first" should be used in 1 environment with 1 feature
    Then The feature from the repository has strategies
      | position | value | percentage |
      | 1        | true  | 15         |
    Then the feature flag has an portfolio strategy "first" which has a value of "true"
    And I get the feature history
    When I attach portfolio strategy "second" to the current environment feature value with the value "true" with percentage 25
    Then The feature from the repository has strategies
      | position | value | percentage |
      | 1        | true  | 15         |
      | 2        | true  | 25         |
    And I expect the portfolio strategy "second" to be attached to the feature history with the value set to "true"
    And I save the current feature value's history using the key "phred"
    And there is an portfolio strategy called "first" in the current environment feature value
    And I swap the order of portfolio strategies "first" and "second" they remain swapped
    And I get the feature history
    And I delete the portfolio strategy called "second" from the current environment feature value
    And I expect the portfolio strategy "second" to be removed from the feature history
    And I find the feature history and compare it to "phred" and it is the same
    Then The feature from the repository has strategies
      | position | value | percentage |
      | 1        | true  | 15         |
