Feature: All flag based functionality works as expected

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  Scenario: A new portfolio with a boolean feature is retired and no longer exists
    Given There is a new feature flag
    Then the feature flag is locked and off
    When I retire the feature flag
    Then there is no feature flag

  Scenario: A new portfolio with a boolean feature is then deleted and it no longer exists
    Given There is a new feature flag
    Then the feature flag is locked and off
    When I delete the feature
    Then there is no feature flag

    @flags
  Scenario: A new portfolio with a boolean feature
#    Given I connect to the Edge server using <ConnectionType>
    Given There is a new feature flag
    Then the feature flag is locked and off
    When I unlock the feature
    Then the feature flag is unlocked and off
    And I set the feature flag to on
    Then the feature flag is unlocked and on
    And I set the feature flag to off
    And I lock the feature
    Then the feature flag is locked and off
    And I unlock the feature
    Then I add a strategy X with no percentage and value on
      | Field   | Type   | Conditional | Values               |
      | userkey | STRING | EQUALS      | user1@mailinator.com |
    Then the feature flag is unlocked and off
    Then I set the context to
      | Field   | Value                |
      | userkey | user1@mailinator.com |
    And the feature flag is unlocked and on
    Then I set the context to
      | Field   | Value                |
      | userkey | user2@mailinator.com |
    And the feature flag is unlocked and off
    And I clear the context
#    Examples:
#      | ConnectionType   |
#      | sse-client-eval  |
#      | poll-client-eval |
#      | poll-server-eval |

