@allvariants @streamingvariants
Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    Given I create application strategies
      | name   | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | first  | _          | _                    | customer  | equals      | brand  | string |
      | second | _          | _                    | customer  | equals      | market | string |
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    And I get the feature history
    When I attach application strategy "first" to the current environment feature value with the value "true"
    And I expect the application strategy "first" to be attached to the feature history with the value "true"
    And the feature history for application strategy "first" has the rules
      | fieldName | conditional | values | type   |
      | customer  | equals      | brand  | string |
    And the feature history for application strategy "first" has no percentage
    And the application strategy "first" should be used in 1 environment with 1 feature
    Then the feature flag has an application strategy "first" which has a value of "true"
    And I get the feature history
    When I attach application strategy "second" to the current environment feature value with the value "true"
    And I expect the application strategy "second" to be attached to the feature history with the value "true"
    And the feature history for application strategy "second" has the rules
      | fieldName | conditional | values | type   |
      | customer  | equals      | market | string |
    And the feature history for application strategy "first" has the rules
      | fieldName | conditional | values | type   |
      | customer  | equals      | brand  | string |
    And I save the current feature value's history using the key "phred"
    And there is an application strategy called "first" in the current environment feature value
    And I swap the order of application strategies "first" and "second" they remain swapped
    And I get the feature history
    And I delete the application strategy called "second" from the current environment feature value
    And I expect the application strategy "second" to be removed from the feature history
    And I find the feature history and compare it to "phred" and it is the same
#    And I attach application strategy "first" to the current environment feature value
#    And I set the feature flag to off and locked

  @appstrat @strathistory
  Scenario: The feature history shows the rules and percentages of application strategies attached to a feature value
    Given I create application strategies
      | name     | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | split    | 200000     | userkey              | region    | equals      | nz,au  | string |
      | split    | 200000     | userkey              | platform  | equals      | ios    | string |
      | override | 300000     | _                    | customer  | equals      | brand  | string |
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    When I attach application strategy "split" to the current environment feature value with the value "true"
    Then I expect the application strategy "split" to be attached to the feature history with the value "true"
    And the feature history for application strategy "split" has the rules
      | fieldName | conditional | values | type   |
      | region    | equals      | nz,au  | string |
      | platform  | equals      | ios    | string |
    And the feature history for application strategy "split" has percentage 20
    When I attach application strategy "override" to the current environment feature value with the value "true" with percentage 15
    Then I expect the application strategy "override" to be attached to the feature history with the value "true"
    And the feature history for application strategy "override" has percentage 15
    And the feature history for application strategy "override" has the rules
      | fieldName | conditional | values | type   |
      | customer  | equals      | brand  | string |
    And the feature history for application strategy "split" has percentage 20

  @appstrat @strathistory
  Scenario: The feature history keeps the name and rules of an application strategy after it is archived
    Given I create application strategies
      | name    | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | retired | _          | _                    | customer  | equals      | gone   | string |
    And There is a new feature flag
    And I set the feature flag to on and unlocked
    When I attach application strategy "retired" to the current environment feature value with the value "true"
    Then I expect the application strategy "retired" to be attached to the feature history with the value "true"
    And I save the current feature value's history using the key "before-archive"
    When I archive the application strategy "retired"
    Then I expect the application strategy "retired" to be removed from the feature history
    And the feature history saved as "before-archive" still has the application strategy "retired" with the rules
      | fieldName | conditional | values | type   |
      | customer  | equals      | gone   | string |
    And I find the feature history and compare it to "before-archive" and it is the same
