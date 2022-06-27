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

  @featuredelete
  Scenario Outline: When I create multiple features and delete them, the MR list of features should match the Edge list of features
    Given I create a new portfolio
    And I create an application
    And I create a new environment
    And I create a service account and read permissions based on the application environments
    And I connect to the feature server
    And There is a new feature flag
    Then the feature flag is locked and off
    Then there are 1 features
#    And I sleep for 30 seconds
    And I delete the feature
    Then there are 0 features
    Given There is a new string feature
    Then the string feature is unlocked and null
    Then there are 1 features
    And I delete the feature
    Then there are 0 features
    Given There is a new number feature
    Then the number feature is unlocked and null
    And I set the number feature value to 15
    Then there are 1 features
    And There is a new feature flag
    Then the feature flag is locked and off
    Then there are 2 features
    And There is a new feature flag
    Then the feature flag is locked and off
    Then there are 3 features
    And I delete the feature
    Then there are 2 features
    Examples: # run it 5 times
      | x |
      | 1 |
      | 1 |
      | 1 |
      | 1 |
      | 1 |

