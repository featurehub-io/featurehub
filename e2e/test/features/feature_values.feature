Feature: Create feature values

  Scenario Outline: I can set feature values per environment if I am given permissions
    Given I ensure a portfolio named "<portfolio>" with description "business application" exists
    And I have a fully registered person "Sebastian" with email "seb@mailinator.com" and password "password123"
    And The superuser is the user
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName2>" with description "<envDesc2>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName3>" with description "<envDesc3>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
#    add superuser and regular user to the group to they have feature permissions
    When I add the user "superuser@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    When I add the user "seb@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    When I can login as user "seb@mailinator.com" with password "password123"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName2>" for feature "<featureKey>"
    And I set the boolean feature value as "true" for environment "<envName3>" for feature "<featureKey>"
    And I ensure the boolean feature value is "true" for environment "<envName>" for feature "<featureKey>"
    And I ensure the boolean feature value is "false" for environment "<envName2>" for feature "<featureKey>"
    And I ensure the boolean feature value is "true" for environment "<envName3>" for feature "<featureKey>"


    Examples:
      | appName      | appDesc           | portfolio      | adminGroup                    | featureKey     | alias         | featureName    | link                           | envName | envDesc    | envName2 | envDesc2 | envName3 | envDesc3 | valueType |
      | FeatureTest1 | FeatureTest1 Desc | Feature Values | Feature Values Administrators | FEATURE_SAMPLE | sssshhhh      | Sample feature | http://featurehub.dev          | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | FeatureTest1 | FeatureTest1 Desc | Feature Values | Feature Values Administrators | NEW_BUTTON     | little_secret | New button     | http://featurehub.dev/new      | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | FeatureTest1 | FeatureTest1 Desc | Feature Values | Feature Values Administrators | NEW_BOAT       | not_secret    | New boat       | http://featurehub.dev/new/boat | prod    | production | test     | test env | dev      | dev env  | boolean   |


  Scenario Outline: I create a random portfolio, with a well known application then two environments, a feature and two environments and all feature flags should exist and be set to false
    Given The superuser is the user
    And I have a randomly named portfolio with the prefix "feature_env_test"
    And I create an application with the name "<appName>"
    And I create an environment "<envName>"
    And I create an environment "<envName2>"
    And I create a feature flag "<feature1>"
    And I create a feature flag "<feature2>"
    And I create an environment "<envName3>"
    And I create an environment "<envName4>"
    And I create a feature flag "<feature3>"
    And I create a feature flag "<feature4>"
    Then there should be 5 environments
    And all environments should have 4 feature flags
    And all feature flags for environment "<envName>" should be "false"
    And all feature flags for environment "<envName4>" should be "false"
    And all feature flags for environment "<envName2>" should be "false"
    And all feature flags for environment "<envName3>" should be "false"
    And all feature flags for environment "production" should be "false"

    Examples:
      | appName | envName | envName2 | envName3 | envName4 | feature1  | feature2  | feature3  | feature4  |
      | nusella | dev1    | dev2     | test1    | test3    | FEATURE_1 | FEATURE_2 | FEATURE_3 | FEATURE_4 |



  Scenario Outline: I can set feature values per environment where the user doesn't have access to some of them
    Given I ensure a portfolio named "<portfolio>" with description "business application" exists
    And The superuser is the user
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName2>" with description "<envDesc2>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName3>" with description "<envDesc3>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    When I add the user "superuser@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName2>" for feature "<featureKey>"
    And I ensure the boolean feature value is "true" for environment "<envName>" for feature "<featureKey>"
    And I ensure the boolean feature value is "false" for environment "<envName2>" for feature "<featureKey>"
    When I ensure a portfolio "<portfolio>" has created a group called "<group>"
    And I ensure all permissions added to the group "<group>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<group>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
#    add user to the group with permissions restricted for some envs.
    And I have a fully registered person "John" with email "John@mailinator.com" and password "password123"
    When I add the user "John@mailinator.com" to the group "<group>" in the portfolio "<portfolio>"
    When I can login as user "John@mailinator.com" with password "password123"

    Examples:
      | appName      | appDesc           | portfolio          | adminGroup                        | group   | featureKey     | alias         | featureName    | link                           | envName | envDesc    | envName2 | envDesc2 | envName3 | envDesc3 | valueType |
      | FeatureTest1 | FeatureTest1 Desc | Feature restricted | Feature restricted Administrators | testers | FEATURE_SAMPLE | sssshhhh      | Sample feature | http://featurehub.dev          | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | FeatureTest1 | FeatureTest1 Desc | Feature restricted | Feature restricted Administrators | testers | NEW_BUTTON     | little_secret | New button     | http://featurehub.dev/new      | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | FeatureTest1 | FeatureTest1 Desc | Feature restricted | Feature restricted Administrators | testers | NEW_CAR        | not_secret    | New car        | http://featurehub.dev/new/boat | prod    | production | test     | test env | dev      | dev env  | string   |
      | FeatureTest1 | FeatureTest1 Desc | Feature restricted | Feature restricted Administrators | testers | NEW_HOUSE      | not_secret    | New house      | http://featurehub.dev/new/boat | prod    | production | test     | test env | dev      | dev env  | string   |

    # this scenario breaks the rules and depends on the data from above
    Scenario: I get the feature values from an application and set them and check they are correct
      Given The superuser is the user
      And I choose the application "FeatureTest1" in portfolio "Feature restricted"
      When I get the details for feature "NEW_HOUSE" and set them as follows:
        | envName | value |
        | test    | false |
      And I get the details for feature "NEW_BUTTON" and set them as follows:
        | envName | value |
        | prod    | false |
      And I get the details for feature "NEW_HOUSE" and set them as follows:
        | envName | value |
        | prod    | true |
