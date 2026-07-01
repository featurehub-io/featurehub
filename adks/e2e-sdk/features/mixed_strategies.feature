@allvariants @streamingvariants
Feature: We can have a mixed set of strategies against a feature value and it propagates to the SDK and is evaluated as expected

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @mixedstrat
  Scenario: A string feature value with many different strategies works as expected
    Given There is a feature string with the key lingling
    And I get the default environment
    And I set the feature flag to unlocked
    And I set the feature value to kwong
    And I create a feature group "always-wonder"
      | key      | value  |
      | lingling | goldie |
    And I update the strategy in the feature group
      | name | percentage | percentageAttributes | fieldName | conditional | values  | type   |
      | ling | _          | _                    | customer  | equals      | android | string |
    And I create application strategies
      | name  | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | white | _          | _                    | customer  | equals      | brand  | string |
    When I attach application strategy "white" to the current environment feature value with the value "jade"
    And I create portfolio strategies
      | name  | percentage | percentageAttributes | fieldName | conditional | values    | type   |
      | black | _          | _                    | customer  | equals      | scrunchie | string |
    And I attach portfolio strategy "black" to the current environment feature value with the value "pearl"
    And I expect the application strategy "white" to be attached to the feature history with the value "jade"
    And I expect the portfolio strategy "black" to be attached to the feature history with the value set to "pearl"
    Then The feature from the repository has the default value "kwong"
    Then The feature from the repository has strategies
      | position | value  |
      | 1        | jade   |
      | 2        | pearl  |
      | 3        | goldie |
    Then I set the context to
      | Field    | Value   |
      | customer | android |
    And The flag in the sdk has a value "goldie"
    Then I set the context to
      | Field    | Value |
      | customer | brand |
    And The flag in the sdk has a value "jade"
    Then I set the context to
      | Field    | Value     |
      | customer | scrunchie |
    And The flag in the sdk has a value "pearl"


