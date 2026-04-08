@allvariants  @streamingvariants
Feature: I am able to use feature filters

  Background:
    Given I create a new portfolio
    And I create an application

  @filters_super  @filters
  Scenario: A superuser is able to create filters and assign them to features
    Given I am a superuser
    Given I create a new feature filter called "client"
    And I create a new feature filter called "server"
    When I ask for feature filters I get "client,server"
    When I ask for feature filters "client" I get "client"
    And I create a feature flag "orm" with the filters "client,server"
    Then the feature flag "orm" contains the filters "client,server" when I get it by key
    And I create a feature flag "ling" with the filters "client"
    Then the feature filters "client" have features attached "orm,ling"
    Then the feature filters "server" have features attached "orm"
    Given I create an application "kwong-kee-roast"
    When I create a feature flag "kwong" with the filters "server"
    Then the feature filters "server" have features attached "orm,kwong"
    Given I restore the previous application
    When I update the feature flag "ling" with the filters "client,server"
    Then the feature filters "server" have features attached "orm,kwong,ling"
    And I create a new feature filter called "egg-roll"
    When I update the feature flag "ling" with the filters "egg-roll"
    Then the feature filters "server" have features attached "orm,kwong"

  @filters_user @filters
  Scenario: A person with feature creation roles in the application can manipulate feature filters
    Given I create a new user
    And I create a new group with application roles "FEATURE_EDIT_AND_DELETE"
    And I assign the new user to the new group
    And I am the created user
    Given I create a new feature filter called "client"
    When I ask for feature filters I get "client"
    And I create a feature flag "ling" with the filters "client"
    Then the feature filters "client" have features attached "ling"

  @filters_portfolio_admin @filters
  Scenario: A portfolio admin person with feature creation roles in the application can manipulate feature filters
    Given I create a new user
    And I get the portfolio admin group
    And I assign the new user to the new group
    And I am the created user
    Given I create a new feature filter called "client"
    When I ask for feature filters I get "client"
    And I create a feature flag "orm" with the filters "client"
    Then the feature filters "client" have features attached "orm"

  @filters_service_account @filters
  Scenario: I can create a service account with associated filters
    Given I create a new feature filter called "always wonder"
    When I create a new service account called "Keep Silent" with feature filters "always wonder"
    Then I can see the feature filter "always wonder" contains the service accounts "Keep Silent"
    Given I create a new feature filter called "omuandco"
    When I update the service account called "Keep Silent" with feature filters "always wonder,omuandco"
    Then I can see the feature filter "omuandco" contains the service accounts "Keep Silent"
