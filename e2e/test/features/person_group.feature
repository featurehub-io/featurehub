Feature: This tests a persons interaction with groups

  Scenario: Ensure that the person search returns groups
    Given I ensure that a portfolio "Testing Groups" has been deleted
    And I ensure a portfolio named "Testing Groups" with description "Testing Groups" exists
    # this creates the user and logs them on, which we don't want
    And I have a fully registered person "Mr Archibald Plate McFarlane Toadstool" with email "hamish@mailinator.com" and password "password123"
    And The superuser is the user
    And I ensure a portfolio "Testing Groups" has created a group called "Developer Group"
    When I add the user "hamish@mailinator.com" to the group "Developer Group" in the portfolio "Testing Groups"
    Then Searching for user should include the group


  Scenario Outline: Ensure that the person search returns groups after roles have been assigned to their group
    Given I ensure that a portfolio "<portfolio>" has been deleted
    And I ensure a portfolio named "<portfolio>" with description "Alpha users" exists
    # this creates the user and logs them on, which we don't want
    And I have a fully registered person "Romeo" with email "<email>" and password "password123"
    And The superuser is the user
    And I ensure a portfolio "<portfolio>" has created a group called "<group>"
    When I add the user "<email>" to the group "<group>" in the portfolio "<portfolio>"
    Then Searching for user should include the group
    And Searching for the group should include the user
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure the permission "EDIT" is added to the group "<group>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    Then Searching for user should include the group


    Examples:
      | portfolio | group         | appName | appDesc   | envName | envDesc    | email                |
      | Italians  | made in italy | Cars    | cool cars | prod    | production | romeo@mailinator.com |

    @superuser
  Scenario Outline: Ensure that a person in an application role in one group and a read in another group has expected access
    And The superuser is the user
    Given I ensure a portfolio named "<portfolio>" with description "Alpha users" exists
    And I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I have a fully registered person "Ivanka" with email "<email>" and password "<password>"
    And The superuser is the user
    And I ensure that an environments exist:
      | name    | desc    |
      | dev     | devenv  |
      | test     | devenv  |
    And I ensure the following group setup exists:
      | groupName | groupDesc |
      | group1    | group1    |
      | group2    | group2    |
    And I add the application role "FEATURE_EDIT" to the group called "<group>"
    And I ensure the permission "READ" is added to the group "group2" to environment "dev"
    And I ensure the permission "READ" is added to the group "group1" to environment "test"
    And I can login as user "<email>" with password "<password>"
    And I load the person "<email>"

    Examples:
      | portfolio | group         | appName | appDesc   | email                  | group  | password   |
      | Italians  | made in italy | Cars    | cool cars | ivanka1@mailinator.com | group1 | <password> |
