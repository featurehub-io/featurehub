Feature: User state should allow various useful bits of info to be stored against the user
  in a structured fashion

  Scenario: We can store hidden environments
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "user_state_hidden"
    And I create an application with the name "state_hidden"
    And I ensure that an environments exist:
      | name            | desc  |
      | dev             | dev   |
      | test            | test  |
      | fried-elephants | fried |
    And I select the environments to hide "production,dev,test,fried-elephants" for the current application
    When I try and store hidden environments
    Then I get the stored hidden environment ids and they are the same


  Scenario: A random user with no permission to an application cannot store environments
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "user_state_hidden"
    And I create an application with the name "state_hidden"
    And I select the environments to hide "production" for the current application
    And I have a randomly generated person with the start of name "Guilliam"
    Then I cannot try and store hidden environment for the current application
