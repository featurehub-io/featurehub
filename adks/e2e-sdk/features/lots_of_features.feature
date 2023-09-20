@allvariants @streamingvariants
Feature: I setup a lot of features

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @lotsoffeatures
  Scenario: I setup the main scenario
    When I setup 47 random feature flags
    And I setup 83 random feature strings
    And I setup 27 random feature numbers
    And I setup 62 random feature json
