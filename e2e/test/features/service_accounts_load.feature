Feature: This should test the loading of new service accounts

  Scenario Outline: I can create a new portfolio and groups 3

    # superuser has to create the portfolio and create the user
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    And I have a fully registered person "<name>" with email "<email>" and password "password123"
    # previous step swapped to that user, so swap back to superuser
    And the first superuser is used for authentication
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    # lets swap to the newly created user and do the rest as them
    When I can login as user "<email>" with password "password123"
    When I ensure a portfolio "<portfolio>" has created a service account called "<service_account>"
    Then portfolio "<portfolio>" has service account "<service_account>"

    Examples:
      | portfolio   | portfolio_desc | service_account | name                    | email                                  |
      | Marketing   | The Fluffers   | SA001           | Marketing Admin         | marketing-admin@mailinator.com         |
#      | Marketing   | The Fluffers   | SA002           | Marketing Admin         | marketing-admin@mailinator.com         |
#      | Engineering | The Eloi       | SA003           | Engineering Admin       | engineering-admin@mailinator.com       |
#      | Engineering | The Eloi       | SA004           | Engineering Admin       | engineering-admin@mailinator.com       |
#      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       |
#      | Platform    | The Morlocks   | SA006           | Platform Admin          | platform-admin@mailinator.com          |
#      | Platform    | The Morlocks   | SA007           | platform testers        | platform_testers-admin@mailinator.com  |
#      | Platform    | The Morlocks   | SA008           | platform_business_users | platform_product-owners@mailinator.com |


  Scenario Outline: I can create a new portfolio and service account and add permissions

    # superuser has to create the portfolio and create the user
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    And I have a fully registered person "<name>" with email "<email>" and password "password123"
    # previous step swapped to that user, so swap back to superuser
    And the first superuser is used for authentication
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    When I ensure a portfolio "<portfolio>" has created application called "<app>" and environment "<environment>" and service account called "<service_account>" and permission type "<permission_type>"
    Then portfolio "<portfolio>" has service account "<service_account>" and the permission "<permission_type>" for this "<app>" and "<environment>"

    Examples:
      | portfolio   | portfolio_desc | service_account | name                    | email                                  | permission_type | app          | environment |
      | Marketing   | The Fluffers   | SA001           | Marketing Admin         | marketing-admin@mailinator.com         | READ            | My App 1     | dev         |
      | Marketing   | The Fluffers   | SA002           | Marketing Admin         | marketing-admin@mailinator.com         | READ            | My App 2     | dev         |
      | Engineering | The Eloi       | SA003           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Wicked App 1 | prod        |
      | Engineering | The Eloi       | SA003           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Wicked App 1 | staging     |
      | Engineering | The Eloi       | SA003           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Wicked App 1 | dev         |
      | Engineering | The Eloi       | SA004           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Wicked App 2 | uat         |
      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Fun app      | dev         |
      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Fun app      | sit         |
      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Fun app      | staging     |
      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       | READ            | Fun app      | uat         |
      | Engineering | The Eloi       | SA005           | Engineering Admin       | engineering-admin@mailinator.com       | CHANGE_VALUE    | Fun app      | production  |
      | Platform    | The Morlocks   | SA006           | Platform Admin          | platform-admin@mailinator.com          | READ            | CD App 1     | sit         |
      | Platform    | The Morlocks   | SA007           | platform testers        | platform_testers-admin@mailinator.com  | READ            | CD App 2     | dev         |
      | Platform    | The Morlocks   | SA008           | platform_business_users | platform_product-owners@mailinator.com | READ            | SER fun      | prod        |


  Scenario Outline: I can reset api keys for a service account as a super user

   # superuser has to create the portfolio and create the user
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    And I have a fully registered person "<name>" with email "<email>" and password "password123"
    # previous step swapped to that user, so swap back to superuser
    And the first superuser is used for authentication
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    When I ensure a portfolio "<portfolio>" has created application called "<app>" and environment "<environment>" and service account called "<service_account>" and permission type "<permission_type>"
    Then portfolio "<portfolio>" has service account "<service_account>" and the permission "<permission_type>" for this "<app>" and "<environment>"
    Then I should be able to reset client keys for service account "<service_account>" for portfolio "<portfolio>" for application "<app>" for environment "<environment>"
    Then I should be able to reset server keys for service account "<service_account>" for portfolio "<portfolio>" for application "<app>" for environment "<environment>"

    Examples:
      | portfolio   | portfolio_desc | service_account | name                    | email                                  | permission_type | app          | environment |
      | Marketing   | The Fluffers   | SA001           | Marketing Admin         | marketing-admin@mailinator.com         | READ            | My App 1     | dev         |

  Scenario Outline: I can reset api keys for a service account as a portfolio admin

   # superuser has to create the portfolio and create the user
    Given I ensure a portfolio named "<portfolio>" with description "<portfolio_desc>" exists
    And I have a fully registered person "<name>" with email "<email>" and password "password123"
    # previous step swapped to that user, so swap back to superuser
    And the first superuser is used for authentication
    And I ensure the "<email>" user is added to the portfolio admin group for "<portfolio>"
    When I ensure a portfolio "<portfolio>" has created application called "<app>" and environment "<environment>" and service account called "<service_account>" and permission type "<permission_type>"
    Then portfolio "<portfolio>" has service account "<service_account>" and the permission "<permission_type>" for this "<app>" and "<environment>"
    When I can login as user "<email>" with password "password123"
    Then I should be able to reset client keys for service account "<service_account>" for portfolio "<portfolio>" for application "<app>" for environment "<environment>"
    Then I should be able to reset server keys for service account "<service_account>" for portfolio "<portfolio>" for application "<app>" for environment "<environment>"

    Examples:
      | portfolio   | portfolio_desc | service_account | name                    | email                                  | permission_type | app          | environment |
      | Marketing   | The Fluffers   | SA001           | Marketing Admin         | marketing-admin@mailinator.com         | READ            | My App 1     | dev         |


