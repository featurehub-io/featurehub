@needs-webserver @streamingvariants
Feature: Webhooks work as expected

  @webhook @webhooks
  Scenario: I setup and test a webhook
    Given I am logged in and have a person configured
    And I create a new portfolio
    And I create an application
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I wait for 5 seconds
    And I update the environment for feature webhooks
    And I wait for 5 seconds
    And I test the webhook
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is locked and off
    And I set the feature flag to unlocked and on
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is unlocked and on
    And I set the feature flag to unlocked and off
    Then we receive a webhook with FEATURE_TITLE_TO_UPPERCASE flag that is unlocked and off
    And we should have 3 messages in the list of webhooks

  @webhook2 @webhooks
  Scenario: I test a webhook that is triggered by a TestSDK changing a value
    Given I create a new portfolio
    And I create an application
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I update the environment for feature webhooks
    And I create a service account and full permissions based on the application environments
    And I use the Test SDK to update feature FEATURE_TITLE_TO_UPPERCASE to unlocked and off
    And we receive a webhook that has changed the feature FEATURE_TITLE_TO_UPPERCASE that belongs to the Test SDK

  @webhook3 @webhooks
  Scenario: I want to see if we can cause a webhook to fail
    Given I create a new portfolio
    And I create an application
    And I update the environment for feature webhooks
    When then I test the webhook
      | feature_type | key   | action     | value | expected_features |
      | flag         | flag1 | justcreate | true  | flag1             |
      | number       | num1  | create     | 15.3  | flag1,num1        |
      | number       | num1  | delete     | none  | flag1             |
#      | flag         | flag1 | change | false |
#      | number       | num1  | create | 17.2  |
#      | number       | num1  | change | 13.1  |
