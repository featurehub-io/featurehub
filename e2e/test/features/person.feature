Feature: General person scenarios

  Scenario: A user can be deleted and undeleted
    Given I have a randomly generated person with the start of name "McGuffin"
    When I delete the user
    Then I cannot find the person when I search for them
    And I can find the when I search for them including archived users
    When I undelete the user
    Then I can find the person when I search for them


