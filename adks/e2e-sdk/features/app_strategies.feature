Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    Given I create an application strategy tagged "first"
    And There is a new feature flag
    When I attach application strategy "first" to the current environment feature value
    Then I create an application strategy tagged "second"
    When I attach application strategy "second" to the current environment feature value
    And there is an application strategy called "first" in the current environment feature value

