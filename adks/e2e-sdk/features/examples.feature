@allvariants @streamingvariants
Feature: I setup the example features to be able to run the examples

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a new feature filter called "client"
    And I create a new feature filter called "server"
    And I create a new feature filter called "marketing"
    And I create a new feature filter called "billing"
#    And I update the environment for feature webhooks
    And I create a service account called "AW Client" with named permissions "read" with current environment
    When I update the service account called "AW Client" with feature filters "billing,client"
    And I connect to the feature server

  @demoexamples
  Scenario: I setup the main scenario
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I set the feature flag to unlocked and on
    When I create a feature flag "uppercase_text" with the filters "client,server"
    And I set the feature flag to unlocked and on
    And There is a string feature named FEATURE_STRING
    And There is a string feature named text_colour
    And There is a string feature named SUBMIT_COLOR_BUTTON
    And There is a number feature named FEATURE_NUMBER
    And There is a json feature named FEATURE_JSON
    Then I write out a feature-examples config file

