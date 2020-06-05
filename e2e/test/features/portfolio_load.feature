Feature: This should test the loading of new portfolios

  @superuser
  Scenario Outline: I can create a new portfolio and assign a new user to it
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    When I have a fully registered superuser "<name>" with email "<email>" and password "<password>"
    Then I can login as user "<email>" with password "<password>"
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    And I update the persons data from the host
    And I can see the user has access to the portfolio

    Examples:
      | portfolio             | portfolio_desc      | email                  | name                         | password   |
      | Engineering Services  | Calories R Us       | chicken@mailinator.com | Chick-a-dee                  | chicken123 |
      | Digital Client Facing | The Best Way To Hug | shrug@mailinator.com   | Enjoy & Shrug with your team | shrug123   |
      | Business Tools        | MongoMe             | cashdb@mailinator.com  | DBs Into Cash                | cash123    |


  Scenario: I can rename a portfolio group and its admin group will also rename
    Given I ensure that a portfolio "Cake Bakers" has been deleted
    And I ensure that a portfolio "General Bakers" has been deleted
    And I ensure a portfolio named "Cake Bakers" with description "Bakers of Cake" exists
    Then there is a admin group called "Cake Bakers Administrators" in portfolio "Cake Bakers"
    And I update the portfolio group "Cake Bakers" to the name "General Bakers" with the description "Bakers of Delight"
    And I ensure a portfolio named "General Bakers" with description "Bakers of Delight" exists
    Then there is a admin group called "General Bakers Administrators" in portfolio "General Bakers"

  Scenario: I cannot create duplicate named portfolios
    Given I ensure that a portfolio "Battery Makers" has been deleted
    And I ensure a portfolio named "Battery Makers" with description "Bakers of Batteries" exists
    Then I cannot create a portfolio named "Battery Makers" with description "Bakers of Batteries"

  Scenario: I cannot update a portfolio to have the same name as another portfolio
    Given I ensure that a portfolio "Battery Makers" has been deleted
    And I ensure a portfolio named "Battery Makers" with description "Bakers of Batteries" exists
    And I ensure a portfolio named "Plastic Makers" with description "Bakers of Plastic" exists
    Then I cannot rename portfolio "Plastic Makers" to "Battery Makers"

  Scenario: I can update my own description
    Given I ensure that a portfolio "Battery Makers" has been deleted
    And I ensure a portfolio named "Battery Makers" with description "Bakers of Batteries" exists
    And I update the portfolio group "Battery Makers" to the name "Battery Makers" with the description "Bakers of Electrolytes"
