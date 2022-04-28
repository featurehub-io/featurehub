Feature: Admin Service accounts can be created, archived, updated and reset

  Background: We create a new portfolio, application, group and feature
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "adminsa"
    And I create an application with the name "AdminSample"
    And I add a group with name "UserGroup"
    And I give all permissions to the environment "production"
    And I create a admin service account called "Treebeard"
    And I add the shared person to the shared group
    And I create a feature flag "SHINING"

  Scenario: The admin service account can access the API
    When I have set the admin service account as the user
    Then I can list portfolios
    And I can list applications
    And I get the details for feature "SHINING" and set them as follows:
      | envName       | value |
      | production    | false |

  Scenario: I can search for this user by name
    Then I can find the admin service account "Treebeard" by searching

  Scenario: The admin service account cannot access the API once the token is reset
    When I have set the admin service account as the user
    Then I can list applications
    Then I reset the admin service account token
    And I cannot list the applications
    Then I have set the admin service account as the user
    Then I can list applications

  Scenario: The admin service account cannot access the API once the account is deleted (archived)
    When I have set the admin service account as the user
    Then I can list applications
    Then I delete the user
    And I cannot list the applications
    And I cannot reset the admin service account token
