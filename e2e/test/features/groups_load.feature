Feature: This should test the loading of new groups

  Scenario Outline: I can create a new portfolio and groups 3

    # superuser has to create the portfolio and create the user
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    And I have a fully registered person "<name>" with email "<email>" and password "password123"
    # previous step swapped to that user, so swap back to superuser
    And The superuser is the user
    # they are also the only ones who can add users initially to the portfolio admin group's group
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    # lets swap to the newly created user and do the rest as them
    When I can login as user "<email>" with password "password123"
    When I ensure a portfolio "<portfolio>" has created a group called "<group>"
    Then portfolio "<portfolio>" has group "<group>"

  Examples:
    | portfolio   | portfolio_desc | group                      | name                    | email                                  |
    | Marketing   | The Fluffers   | the fluffers               | Marketing Admin         | marketing-admin@mailinator.com         |
    | Marketing   | The Fluffers   | power pointers             | Marketing Admin         | marketing-admin@mailinator.com         |
    | Engineering | The Eloi       | engineering devs           | Engineering Admin       | engineering-admin@mailinator.com       |
    | Engineering | The Eloi       | engineering testers        | Engineering Admin       | engineering-admin@mailinator.com       |
    | Engineering | The Eloi       | engineering business users | Engineering Admin       | engineering-admin@mailinator.com       |
    | Platform    | The Morlocks   | platform engineers         | Platform Admin          | platform-admin@mailinator.com          |
    | Platform    | The Morlocks   | platform testers           | platform testers        | platform_testers-admin@mailinator.com  |
    | Platform    | The Morlocks   | platform business_users    | platform_business_users | platform_product-owners@mailinator.com |





