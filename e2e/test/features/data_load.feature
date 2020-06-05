Feature: load the sample data via the api

  @dataload
  Scenario Outline: There should be superusers loaded
    Given the system has been initialized
    And I have a fully registered superuser "<name>" with email "<email>" and password "<password>"
    And I can login as user "<email>" with password "<password>"
    Then the user exists and has superuser groups
    Examples:
      | name             | email                  | password    |
      | Капрельянц Ирина | Ирина@mailinator.com   | password123 |
      | Irina Southwell  | irina@mailinator.com   | password123 |
      | Richard Vowles   | richard@mailinator.com | password123 |


  Scenario: The system should be initialized and I should be able to logout
    Given the system has been initialized
    And The superuser is the user
    Then I should be able to logout

