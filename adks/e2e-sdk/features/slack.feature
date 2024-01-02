@needs-webserver
Feature: System supports Slack

  @slack
  Scenario: I setup and test a slack event
    Given I am logged in and have a person configured
    And I create a new portfolio
    And I create an application
    And I update the environment for Slack
    And I wait for 5 seconds
    And I clear cloud events
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    # we have to set it now otherwise it won't trigger
    And I set the feature flag to unlocked and on
    Then I receive a cloud event of type "integration/slack-v1"

