@needs-webserver
Feature: Webhooks work as expected

  @webhook
  Scenario: I setup and test a webhook
    Given I create a new portfolio
    And I create an application
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I update the environment for feature webhooks
    And I test the webhook
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is locked and off
    And I set the feature flag to unlocked and on
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is unlocked and on
    And I set the feature flag to unlocked and off
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is unlocked and off
    And we should have 3 messages in the list of webhooks
