Feature: User state should allow various useful bits of info to be stored against the user
  in a structured fashion

  Scenario Outline: We can store hidden environments
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "user_state_hidden"
    And I create an application with the name "state_hidden"
    When I try and store hidden environment "<envIds>" for the current application
    Then I get the env ids "<envIds>" and they are the same

    Examples:
      | envIds |
      | 1,2,3,4,5 |

  Scenario: A random user with no permission to an application cannot store environments
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "user_state_hidden"
    And I create an application with the name "state_hidden"
    And I have a randomly generated person with the start of name "Guilliam"
    Then I cannot try and store hidden environment for the current application
