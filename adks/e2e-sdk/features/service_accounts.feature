Feature: Service accounts have expected permissions and operation

  @ApiKeys
  Scenario: Normal users can see expected API keys
    Given I create a new portfolio
    And I create an application
    And I create a new environment
    And I create a service account with named permissions "read,extended_data" with current environment
    When I ask for api keys for the application for superuser
    Then the current environment api keys are visible for superuser
    When I create a new normal group
    And I assign roles "read,extended_data" to the group for the current environment
    And I create a new user
    And I assign the new user to the new group
    And I ask for api keys for the application for new user
    Then the current environment api keys are visible for user

