@allvariants @streamingvariants
Feature: All string based functionality works as expected

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

    @strings
  Scenario: A new portfolio with a strings feature
#    Given I connect to the Edge server using <ConnectionType>
    Given There is a new string feature
    Then the string feature is unlocked and null
    When I lock the feature
    Then the string feature is locked and null
    Given I unlock the feature
    And I set the string feature value to junior
    Then the string feature is unlocked and junior
    And I lock the feature
    Then the string feature is locked and junior
    And I unlock the feature
    Then I add a strategy X with no percentage and value senior
      | Field   | Type   | Conditional | Values               |
      | userkey | STRING | EQUALS      | user1@mailinator.com |
    Then the string feature is unlocked and junior
    Then I set the context to
      | Field   | Value                |
      | userkey | user1@mailinator.com |
    And the string feature is unlocked and senior
    Then I set the context to
      | Field   | Value                |
      | userkey | user2@mailinator.com |
    And the string feature is unlocked and junior
    And I clear the context
#    Examples:
#      | ConnectionType   |
#      | sse-client-eval  |
#      | poll-client-eval |
#      | poll-server-eval |
