@allvariants @streamingvariants
Feature: Certain permission combinations should work as expected

  @portfolio-group
  Scenario: The group should be able to identify who is a superuser and who is not
    Given I create a new portfolio
    And I get the portfolio admin group
    And I create a new user
    And I assign the new user to the new group
    And I get the portfolio admin group
    And the superuser is in the group as a superuser
    And the superuser is in the group as a user
    And the user is in the group as a user
    And the user is not in the group as a superuser

  @delete-superuser-from-group
  Scenario: I add the superuser to the portfolio admin group and then remove them
    Given I create a new portfolio
    And I get the portfolio admin group
    Then I cannot delete the superuser from the group
