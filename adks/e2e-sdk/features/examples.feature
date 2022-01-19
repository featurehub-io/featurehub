Feature: I setup the example features to be able to run the examples

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @demoexamples
  Scenario: I setup the main scenario
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I set the feature flag to unlocked and on
    And There is a string feature named FEATURE_STRING
    And There is a number feature named FEATURE_NUMBER
    And There is a json feature named FEATURE_JSON
    Then I write out a feature-examples config file
