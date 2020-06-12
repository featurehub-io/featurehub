Feature: Environments exist under features and should be able to be managed only by super admins and portfolio admins

  Scenario Outline: A new environment is not visible to those outside the portfolio
    Given I ensure a portfolio named "<portfolio>" with description "Simple" exists
    And The superuser is the user
    When I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    Then I can find environment "<envName>" in the application
    And The application has environments
    # creates user and logs on as them
    And I have a fully registered person "Sasha" with email "sasha@mailinator.com" and password "password123"
    And I cannot find environment "<envName>" in the application

    Examples:
      | appName    | appDesc      | portfolio         | envName    | envDesc         |
      | Sample1App | Sample1 Desc | Pizza Dough Guild | Dev        | Development env |
      | Sample1App | Sample1 Desc | Pizza Dough Guild | Staging    | Staging env     |
      | Sample1App | Sample1 Desc | Pizza Dough Guild | Production | Production env  |

  Scenario Outline: A new environment can be seen by those in the groups inside the portfolio
    Given The superuser is the user
    And I ensure a portfolio named "<portfolio>" with description "<desc>" exists
    And I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure a portfolio "<portfolio>" has created a group called "<groupName>"
    And I have a fully registered person "<personName>" with email "<email>" and password "password123"
    And The superuser is the user
    And I ensure the "<email>" user is added to the portfolio group "<groupName>" for portfolio "<portfolio>"
    Then I can login as user "<email>" with password "password123"
    And I can find environment "<envName>" in the application

    Examples:
      | appName    | appDesc      | desc   | portfolio         | envName | envDesc         | groupName         | personName | email                 |
      | Sample1App | Sample1 Desc | Simple | Pizza Dough Guild | Dev     | Development env | #1 PizzaGuys R US | Doughboy   | pizza1@mailinator.com |
      | Sample1App | Sample1 Desc | Simple | Pizza Dough Guild | Dev     | Development env | #2 PizzaGuys R US | Doughboy   | pizza2@mailinator.com |
      | Sample1App | Sample1 Desc | Simple | Pizza Dough Guild | Dev     | Development env | #2 PizzaGuys R US | Doughboy   | pizza3@mailinator.com |

  Scenario Outline: A portfolio user can create environments
    Given The superuser is the user
    And I ensure a portfolio named "<portfolio>" with description "Simple" exists
    And I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I have a fully registered person "<personName>" with email "<email>" and password "password123"
    And The superuser is the user
    And I ensure the "<email>" user is added to the portfolio group "<portfolioGroup>" for portfolio "<portfolio>"
    Then I can login as user "<email>" with password "password123"
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I can find environment "<envName>" in the application

    Examples:
      | appName    | appDesc      | portfolio       | portfolioGroup                 | envName | envDesc         | personName | email                     |
      | VinegarApp | Sample1 Desc | Picalilly Guild | Picalilly Guild Administrators | Dev     | Development env | MustardGuy | mustardguy@mailinator.com |

  Scenario Outline: An environment can be set to be the prior environment of another
    Given The superuser is the user
    And I ensure a portfolio named "<portfolio>" with description "<desc>" exists
    And I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environments exist:
      | name | desc    |
      | dev  | devenv  |
      | test | testenv |
    And I ensure that environment "dev" is before environment "test"
    And I check to see that the prior environment for "test" is "dev"
    And I check to see that the prior environment for "dev" is empty
    And I ensure that environment "test" is before environment "dev"
    And I check to see that the prior environment for "dev" is "test"
    And I check to see that the prior environment for "test" is empty

    Examples:
      | appName    | appDesc      | desc   | portfolio         |
      | Sample1App | Sample1 Desc | Simple | Pizza Dough Guild |

  Scenario Outline: An environment three layer promotion
    Given The superuser is the user
    And I have a randomly named portfolio with the prefix "environment_order_test"
    And I create an application with the name "<appName>"
    And I delete all existing environments
    And I ensure that an environments exist:
      | name | desc    |
      | dev  | devenv  |
      | test | testenv |
      | staging | testenv |
    And I check that environment ordering:
      | parent  | child   |
      | dev        | test    |
      | test       | staging |
      | staging    |         |
    And I ensure that environment "dev" is before environment "test"
    And I ensure that environment "test" is before environment "staging"
    And I check to see that the prior environment for "test" is "dev"
    And I check to see that the prior environment for "staging" is "test"
    And I check to see that the prior environment for "dev" is empty
    And I ensure that environment "staging" is before environment "dev"
    And I check to see that the prior environment for "dev" is "staging"
    And I check to see that the prior environment for "staging" is "test"
    And I check to see that the prior environment for "test" is empty

    Examples:
      | appName    |
      | Sample2App |

  Scenario: A portfolio with an awful lot of environments exists
    Given The superuser is the user
    And I have a randomly named portfolio with the prefix "Lots Of Envs"
    And I create an application with the name "Layout like a champ"
    And I ensure that an environments exist:
      | name                             | desc                                      |
      | dev                              | devenv                                    |
      | devirina                         | dev for irina                             |
      | devrichard                       | dev for richard                           |
      | test                             | testenv                                   |
      | test-maws                        | test for mary anne                        |
      | test-aleks                       | test for aleks                            |
      | production-debug-JIRA-123        | sanitized prodouction for JIRA-123        |
      | production-debug-JIRA-789        | sanitized prodouction for JIRA-789        |
      | production-debug-JIRA-153        | sanitized prodouction for JIRA-153        |
      | uat                              | uat environment                           |
      | staging                          | testenv                                   |
      | customer-marketing               | customer marketing demo env               |
      | customer-training                | customer training env                     |
      | integration-portal               | integration testing environment           |
      | penetration-testing              | penetration testing environment           |
      | companyx-new-feature-integration | development of feature with new company x |
    And I create a feature flag "FEATURE_NEW_LAYOUT"
    And I create a feature flag "FEATURE_COMPANYX"
    And I create a feature flag "FEATURE_INTEGRATION_ENHANCEMENT_BOB"
    And I create a feature flag "FEATURE_LAYOUT_ALEKS"



