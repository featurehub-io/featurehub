Feature: I am able to use feature filters

  Background:
    Given I create a new portfolio
    And I create an application

  @filters_super
  Scenario: A superuser is able to create filters and assign them to features
    Given I create a new feature filter called "client"
    And I create a new feature filter called "server"
    When I ask for feature filters I get "client,server"
    When I ask for feature filters "client" I get "client"
    And I create a feature flag "orm" with the filters "client,server"
    And I create a feature flag "ling" with the filters "client"
    Then the feature filters "client" have features attached "orm,ling"
    Then the feature filters "server" have features attached "orm"
    Given I create an application "kwong-kee-roast"
    When I create a feature flag "kwong" with the filters "server"
    Then the feature filters "server" have features attached "orm,kwong"

