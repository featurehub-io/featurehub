Feature: We can save, change and retrieve application strategies on feature values

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

  @appstrat
  Scenario: I can create a feature value with an application strategy attached
    # application strategies have a matcher that the attribute "<strategy-name>" must equal "<strategy-name>"
    Given I create an application strategy tagged "first"
    And There is a new feature flag
    And I set the feature flag to off and unlocked
    Then the feature flag is unlocked and off
    When I attach application strategy "first" to the current environment feature value
    And the application strategy "first" should be used in 1 environment with 1 feature
    Then the feature flag with application strategy "first" should be unlocked and on
    Then I create an application strategy tagged "second"
    And I set the feature flag to on and unlocked
    Then the feature flag is unlocked and on
    When I attach application strategy "second" with value "false" to the current environment feature value
    # this uses the fact that the attribute "second" should match "second"
    Then the feature flag with application strategy "second" should be unlocked and off
    And there is an application strategy called "first" in the current environment feature value
    And I swap the order of "first" and "second" they remain swapped
    And I delete the application strategy called "second" from the current environment feature value
    # the strategy second should have gone away so it should revert back to the default
    Then the feature flag with application strategy "second" should be unlocked and on
#    And I attach application strategy "first" to the current environment feature value
#    And I set the feature flag to off and locked
