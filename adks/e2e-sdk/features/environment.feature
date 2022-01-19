Feature: We have a grouping of environmental behaviour around features


  @environment
  Scenario: When I delete an environment, it becomes unpublished from Edge
    Given I create a new portfolio
    And I create an application
    And I create a new environment
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server
    And There is a new feature flag
    Then the feature flag is locked and off
    And I delete the environment
    Then the edge connection is no longer available

