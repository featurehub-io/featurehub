@dacha
Feature: this feature is designed to be run on an empty system and trigger the creation of a single environment
  to ensure the Dacha populates correctly.


  Scenario Outline: A single portfolio with a single application and implicit production environment settles dacha.
    Given The superuser is the user
    And I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    When I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    Then I am able to find application called "<appName>" in the portfolio "<portfolio>"

    Examples:
      | appName   | appDesc                 | portfolio       | portfolio_desc  |
      | Dacha App | Sample Dacha Applicaton | Dacha Portfolio | Dacha Portfolio |
