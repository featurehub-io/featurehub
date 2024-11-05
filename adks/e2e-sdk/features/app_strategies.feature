Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    Given I have an application strategy
    And There is a new feature flag
    When I attach the application strategy to the current environment feature value

