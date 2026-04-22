Feature: support for creating data for use in diagnosing issues

  @demo-two-apps
  Scenario: I create a Portfolio with 2 applications and several flags
    Given I create a new portfolio
    And I create a new feature filter called "client"
    And I create a new feature filter called "server"
    And I create a new feature filter called "marketing"
    And I create a new feature filter called "billing"
    And I create an application "char-siu"
    When I create a feature flag "uppercase_text" with the filters "client,server"
    When I create a feature flag "jira-1-flag" with the filters "client"
    When I create a feature flag "jira-2-flag" with the filters "client"
    When I create a feature flag "jira-3-flag" with the filters "client"
    When I create a feature flag "jira-33-flag" with the filters "client,server"
    When I create a feature flag "jira-4-flag" with the filters "client"
    When I create a feature flag "jira-5-flag" with the filters "client"
    When I create a feature flag "jira-6-flag" with the filters "client"
    When I create a feature flag "jira-66-flag" with the filters "client"
    And I create an application "tofu"
    When I create a feature flag "uppercase_text" with the filters "marketing,server"
    When I create a feature flag "gyro-1-flag" with the filters "marketing"
    When I create a feature flag "gyro-2-flag" with the filters "marketing"
    When I create a feature flag "gyro-3-flag" with the filters "marketing"
    When I create a feature flag "gyro-33-flag" with the filters "marketing,server"
    When I create a feature flag "gyro-4-flag" with the filters "marketing"
    When I create a feature flag "gyro-5-flag" with the filters "marketing"
    When I create a feature flag "gyro-6-flag" with the filters "marketing"
    When I create a feature flag "gyro-66-flag" with the filters "marketing"

