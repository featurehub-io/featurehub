Feature: I create sample data used for documentation. This is not used in the normal test suite.

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments

    # set the env PORTFOLIO_NAME before running this so you can find it
    @docs
  Scenario:
    Given I create portfolio strategies
      | name               | percentage | percentageAttributes | fieldName | conditional | values   | type   |
      | Country - NZ       | _          | _                    | country   | equals      | nz       | string |
      | Country - Thailand | _          | _                    | country   | equals      | thailand | string |
    And I create application strategies
      | name            | percentage | percentageAttributes | fieldName | conditional | values | type   |
      | Company - Ch3   | _          | _                    | company   | equals      | ch3    | string |
      | Company - WeTV  | _          | _                    | company   | equals      | wetv   | string |
      | Company - iQiYi | _          | _                    | company   | equals      | iqiyi  | string |
    Given I create a new user "Booky"
    And I create a new user "Aom"
    And I create a new group "app-users"
    And I can assign portfolio strategy "delete" roles to the group
    And I add the user "Booky" to the group "app-users"
    And I add the user "Aom" to the group "app-users"
    And I create a new feature filter called "client"
    And I create a new feature filter called "server"
    When I create a feature flag "char-siu" with the filters "client,server"
    And I set the feature flag to off and unlocked
    When I attach portfolio strategy "Country - NZ" to the current environment feature value with the value "true" with percentage 15
    When I attach portfolio strategy "Country - Thailand" to the current environment feature value with the value "true"
    When I attach application strategy "Company - Ch3" to the current environment feature value with the value "true"
    When I attach application strategy "Company - iQiYi" to the current environment feature value with the value "true" with percentage 25
    And I create a feature group "always-wonder"
      | key      | value  |
      | char-siu | true |
    And I update the strategy in the feature group
      | name           | percentage | percentageAttributes | fieldName | conditional | values  | type   |
      | android-device | _          | _                    | device    | equals      | android | string |
