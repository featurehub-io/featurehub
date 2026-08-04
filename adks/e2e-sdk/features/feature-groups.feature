@allvariants @streamingvariants
Feature: Feature groups should operate as per definition

  Background:
    Given I create a new portfolio
    And I create an application
    Given I create an environment "saas"
    And I create a service account and read permissions based on the application environments
    And I connect to the feature server

    @fgroups @fgrouparchived
  Scenario: Feature group should be able to update without losing archived feature
      Given there are 0 features
      And There is a feature string with the key chago
      And There is a feature number with the key kwongkee
      And There is a feature json with the key omuandco
      When I create a feature group "ch3group"
        | key    | value |
        | chago  | maria |
        | kwongkee | 2     |
      Then the feature group only has the keys "chago,kwongkee"
      And I delete the feature with the key "chago"
      Then the feature group only has the keys "kwongkee"
      And I update the values of the feature group to
        | key      | value |
        | omuandco | {}    |
        | kwongkee | 2     |
      Then the feature group only has the keys "kwongkee,omuandco"
      When I undelete the feature "chago"
      Then the feature group only has the keys "kwongkee,omuandco,chago"


    @fgroups
  Scenario: New feature groups should expose new strategies
    Given there are 0 features
    And There is a feature string with the key keiko
    And I set the feature value to hillsalive
    And There is a feature number with the key obrien
    And I set the feature value to 6
    Then there are 2 features
    And I create a feature group "saas-group"
      | key    | value |
      | keiko  | maria |
      | obrien | 2     |
    And I update the strategy in the feature group
      | name   | percentage | percentageAttributes | fieldName | conditional | values  | type   |
      | mobile | _          | _                    | device    | equals      | android | string |
    Then the edge repository has a feature "keiko" with a strategy
      | fieldName | values  | expectedValue |
      | device    | android | maria         |
    And the edge repository has a feature "obrien" with a strategy
      | fieldName | values  | expectedValue |
      | device    | android | 2             |
    When I update the strategy in the feature group
      | name    | percentage | percentageAttributes | fieldName | conditional | values      | type   |
      | country | _          | _                    | country   | ne          | new-zealand | string |
    And I update the values of the feature group to
      | key    | value |
      | keiko  | juan  |
      | obrien | 17    |
    Then the edge repository has a feature "keiko" with a strategy
      | fieldName | values    | expectedValue |
      | country   | australia | juan          |
    And the edge repository has a feature "obrien" with a strategy
      | fieldName | values  | expectedValue |
      | country   | germany | 17            |
    And I delete the feature with the key "keiko"
    And the feature group only has the keys "obrien"
    And I undelete the feature "keiko"
    And the feature group only has the keys "obrien,keiko"
    And I update the values of the feature group to
      | key    | value |
      | obrien | 12    |
    Then the edge repository has a feature "keiko" with a strategy
      | fieldName | values    | expectedValue |
      | country   | australia | hillsalive    |
    And the edge repository has a feature "obrien" with a strategy
      | fieldName | values | expectedValue |
      | country   | japan  | 12            |
    And the edge repository has a feature "obrien" with a strategy
      | fieldName | values | expectedValue |
      | country   | new-zealand  | 6            |
    When I delete the feature group "saas-group"
    And the edge repository has a feature "obrien" with a strategy
      | fieldName | values  | expectedValue |
      | device    | android | 6             |
