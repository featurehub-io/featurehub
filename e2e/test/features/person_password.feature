Feature: Testing of features around people changing their passwords

  Scenario Outline: A registered user can change their password
  Given I have a fully registered person "<name>" with email "<email>" and password "<password>"
    When I want to change their password from "<password>" to "<password2>" I can
    And I can login as user "<email>" with password "<password2>"

  Examples:
    | name            | email                 | password    | password2   |
    | Irina Northwell | irina2@mailinator.com | password321 | password123 |
    | Irina Northwell | irina2@mailinator.com | password123 | password321 |
    | Roger Cleghorn  | roger@mailinator.com  | password999 | password123 |
    | Roger Cleghorn  | roger@mailinator.com  | password123 | password999 |
    | James Vowles    | james@mailinator.com  | password624 | password123 |
    | James Vowles    | james@mailinator.com  | password123 | password624 |

    # have to have the paswords in pairs so they run each time

  Scenario Outline: An admin can change a user's name and email address
#    Given The administrator ensures the registered person "<email>" is deleted
#    And The administrator ensures the registered person "<newemail>" is deleted
    And I have a fully registered person "<name>" with email "<email>" and password "<password>"
    And I can login as user "<email>" with password "<password>"
    When I try an update the user "<email>" to a new email "<newemail>" and new name "<newname>" I fail
    And the administrator updates user "<email>" to a new email "<newemail>" and new name "<newname>"
    Then I can login as user "<newemail>" with password "<password>"

  Examples:
    | name  | email                | newemail              | password    | newname |
    | Bruce | bruce@mailinator.com | bruce2@mailinator.com | password123 | Bruke   |

  Scenario Outline: A registered user can have their password reset
    Given I have a fully registered person "<name>" with email "<email>" and password "<password>"
    When the administrator resets the password for "<email>" to "<password2>"
    And I can login as user "<email>" with password "<password2>"
    And the current user's password requires resetting
    Then I cannot list any portfolios
    And then I reset my temporary password to "<password3>"
    And I can list portfolios

    Examples:
      | name         | email                       | password    | password2   | password3   |
      | Joe Eastwell | joe.eastwell@mailinator.com | password321 | password123 | password678 |

  Scenario Outline: A registered user cannot reset their password via replace temp password without being in password reset mode
    And I have a fully registered person "<name>" with email "<email>" and password "<password>"
    And then I cannot reset my temporary password to "<password2>"

    Examples:
      | name        | email                      | password    | password2   |
      | Joe Clinton | joe.clinton@mailinator.com | password321 | password123 |
