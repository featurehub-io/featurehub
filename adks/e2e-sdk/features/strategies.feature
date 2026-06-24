@allvariants @streamingvariants
Feature: We can have a mixed set of strategies against a feature value and it propagates to the SDK and is evaluated as expected

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

    @mixedstrat
  Scenario: A string feature value with many different strategies works as expected
    Given There is a feature string with the key lingling
    And I get the default environment
    And I set the feature flag to unlocked
    And I set the feature value to kwong
    And I create an application strategy tagged "white"
    When I attach application strategy "white" to the current environment feature value with the value "jade"
    And I create an portfolio strategy with the name "black"
    And I attach portfolio strategy "black" to the current environment feature value with the value "pearl"
    And I expect the application strategy "white" to be attached to the feature history with the value "jade"
    And I expect the portfolio strategy "black" to be attached to the feature history with the value set to "pearl"
    Then The feature from the repository has the default value "kwong"
    Then The feature from the repository has a strategy with the value "jade" in position 1
    Then The feature from the repository has a strategy with the value "pearl" in position 2


