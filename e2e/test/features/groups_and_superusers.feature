Feature: This set of scenarios focuses on supers being added to portfolios and not being able to be removed
   or being removed automatically when they are no longer supers.


  Scenario: When a superuser is added to a group directly they should become a member of a portfolio's admin group automatically and reversed
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "superuser_admin_1"
    And I have a randomly generated superuser with the start of name "Cudgel"
    And the first superuser is used for authentication
    And the shared person is a superuser
    When The portfolio admin group contains the current user
    And I remove the user from the superuser group
    And the portfolio admin group does not contain the current user

  Scenario: When we edit the portfolio group members directly to remove a superuser, we can't
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "superuser_admin_2"
    And I have a randomly generated superuser with the start of name "Red"
    And the first superuser is used for authentication
    And the shared person is a superuser
    When The portfolio admin group contains the current user
    And I fail to remove the shared user from the portfolio group
    Then The portfolio admin group contains the current user

  Scenario: When we add via the group members and it is the superuser group, all portfolios get that new user
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "superuser_admin_3"
    And I have a randomly generated person with the start of name "Stark White"
    And the first superuser is used for authentication
    And the portfolio admin group does not contain the current user
    When I add the shared person to the superuser group via the group membership
    Then The portfolio admin group contains the current user

  Scenario: When we remove via the group members and it is the superuser group, all portfolios remove that user
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "superuser_admin_4"
    And I have a randomly generated superuser with the start of name "Wan"
    And the first superuser is used for authentication
    And The portfolio admin group contains the current user
    When I remove the shared person to the superuser group via the group membership
    Then the portfolio admin group does not contain the current user

  Scenario: When a portfolio is created all superusers get added to that portfolio group and cannot be removed via the group members api
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "superuser_admin_5"
    And I have a randomly generated superuser with the start of name "Wotcha"
    And the first superuser is used for authentication
    And I attempt to remove the superuser from the shared portfolio group via the group membership
    And The portfolio admin group contains the current user

