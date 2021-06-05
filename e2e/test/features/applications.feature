Feature: Here we explore the capabilities around Applications inside a portfolio

  Background: I ensure a portfolio exists and the user is logged in who owns it
    Given I ensure a portfolio named "Biscuit Makers" with description "Bakers of Biscuits" exists
    And I have a fully registered person "Brian" with email "brian@mailinator.com" and password "password123"
    And the first superuser is used for authentication
    And I ensure the "brian@mailinator.com" user is added to the portfolio admin group for "Biscuit Makers"
    And I can login as user "brian@mailinator.com" with password "password123"

  Scenario Outline: I can add application to the portfolio
    When I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    Then I am able to find application called "<appName>" in the portfolio "<portfolio>"

    Examples:
      | appName    | appDesc         | portfolio      |
      | World      | World Desc      | Biscuit Makers |
      | Conquering | Conquering Desc | Biscuit Makers |
      | Ideas      | Ideas Desc      | Biscuit Makers |
      | пирожок    | Mince Brioche   | Biscuit Makers |

  Scenario Outline: I can update and delete an application in the portfolio
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "<portfolio>"
    When I create an application with the name "<appName>"
    And I am able to update the application with the name "<appName>" to the name "<appName2>" with the description "<appDesc2>"
    Then I am able to find application called "<appName2>"
    And I delete the application called "<appName2>"
    Then I am not able to find application called "<appName2>"

    Examples:
      | appName     | appName2 | appDesc2    | portfolio      |
      | World7      | World2   | Desc2       | Biscuit Makers |
      | Conquering9 | Pirozhki | Mince Dough | Biscuit Makers |

  Scenario Outline: I can create and update features in an application in a portfolio
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that the feature with the key "<featureKey>" has been removed
    And I ensure that the feature with the key "<featureKey2>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    And I rename the feature with the key "<featureKey>" to "<featureKey2>"
    And I can find the feature with a key "<featureKey2>"
    When I delete the feature with the key "<featureKey2>"
    And I cannot find the feature with a key "<featureKey2>"

    Examples:
      | appName      | appDesc           | portfolio      | featureKey     | featureKey2     | alias    | featureName    | link                  |
      | FeatureTest1 | FeatureTest1 Desc | Biscuit Makers | FEATURE_SAMPLE | FEATURE_SAMPLE2 | sssshhhh | Sample feature | http://featurehub.dev |


  Scenario Outline: A user I create can be given permissions to add and edit a feature
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I have a fully registered person "Katya" with email "<email>" and password "<password>"
    And the first superuser is used for authentication
    And I ensure a portfolio "<portfolio>" has created a group called "<group>"
    And I add the user "<email>" to the group "<group>" in the portfolio "<portfolio>"
    And I add the application role "FEATURE_EDIT" to the group called "<group>"
    And I ensure that an environment "<envName>" with description "<envDesc>" exists
    And I ensure the permission "READ" is added to the group
    And I can login as user "<email>" with password "<password>"
    And I confirm I have the ability to edit features in the current application
    And I ensure that the feature with the key "<featureKey>" has been removed
    Then I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"


    Examples:
      | appName     | appDesc          | portfolio      | featureKey     | alias    | featureName    | link                  | email             | group    | password    | envName | envDesc |
      | PunterTest1 | PunterTest1 Desc | Biscuit Makers | FEATURE_SAMPLE | sssshhhh | Sample feature | http://featurehub.dev | katya@example.com | NPCheque | password123 | KatyEnv | KathEnvDesc |

#    Scenario: I can create two applications in a portfolio and setting group and service permissions is persisted
#      Given I have a randomly named portfolio with the prefix "perm_check"
#      And I create a group called "people"
#      And I create a service account called "sa001"
#      And I create an application with the name "app1"
#      And I set the service account "sa001" permissions on application "app1" to "read"
#      And I create an application with the name "app2"
#      And I set the service account "sa001" permissions on application "app2" to "read,write"
#      And I ensure only permissions "read,write" are set on the group "people" for the env "production" for app "app1" for portfolio "<current>"

