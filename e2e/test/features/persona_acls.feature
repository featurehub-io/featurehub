Feature: Create some personas with different feature access data

  Use this guy to login on the front-end to see different app cases: John@mailinator.com

  Scenario Outline: Set some feature values and give all permissions
    Given I ensure a portfolio named "<portfolio>" with description "persona test" exists
    And the first superuser is used for authentication
    When I ensure a portfolio "<portfolio>" has created a group called "<group>"
    When I add the user "superuser@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName2>" with description "<envDesc2>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName3>" with description "<envDesc3>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
    And I unlock the feature value for environment "<envName>" for feature "<featureKey>"
    And I unlock the feature value for environment "<envName2>" for feature "<featureKey>"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName2>" for feature "<featureKey>"
    And I ensure the boolean feature value is "true" for environment "<envName>" for feature "<featureKey>"
    And I ensure the boolean feature value is "false" for environment "<envName2>" for feature "<featureKey>"
    And I ensure all permissions added to the group "<group>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<group>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
#    add user to the group with permissions restricted for some envs.
    And I have a fully registered person "John" with email "John@mailinator.com" and password "password123"
    When I add the user "John@mailinator.com" to the group "<group>" in the portfolio "<portfolio>"

    Examples:
      | appName                | appDesc           | portfolio    | adminGroup                  | group   | featureKey      | alias    | featureName     | link                  | envName | envDesc    | envName2 | envDesc2 | envName3 | envDesc3 | valueType |
      | Test app - multi envs. | FeatureTest1 Desc | Persona test | Persona test Administrators | Testers | FEATURE_SAMPLE  | sssshhhh | Sample feature  | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | Test app - multi envs. | FeatureTest1 Desc | Persona test | Persona test Administrators | Testers | FEATURE_SAMPLE2 | sssshhhh | Sample feature2 | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | Test app - multi envs. | FeatureTest1 Desc | Persona test | Persona test Administrators | Testers | FEATURE_SAMPLE3 | sssshhhh | Sample feature3 | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | boolean   |
      | Test app - multi envs. | FeatureTest1 Desc | Persona test | Persona test Administrators | Testers | FEATURE_SAMPLE4 | sssshhhh | Sample feature4 | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | boolean   |


  Scenario Outline: Create app with one feature but no environments (user shouldn't see the app in the drop-down)
    Given I ensure a portfolio named "<portfolio>" with description "persona test" exists
    And the first superuser is used for authentication
    And I ensure a portfolio "<portfolio>" has created a group called "Just a portfolio group"
    And I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that the feature with the key "<featureKey>" has been removed
    And I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    And I have a fully registered person "John" with email "John@mailinator.com" and password "password123"
    And I add the user "John@mailinator.com" to the group "Just a portfolio group" in the portfolio "<portfolio>"

    Examples:
      | appName            | appDesc                 | portfolio    | featureKey     | alias    | featureName    | link                  |
      | Test app - no envs | FeatureTest 2 - no envs | Persona test | FEATURE_SAMPLE | sssshhhh | Sample feature | http://featurehub.dev |


  Scenario Outline: Create apps and setup different access, e.g read, lock..
    Given I ensure a portfolio named "<portfolio>" with description "persona test" exists
    And the first superuser is used for authentication
    When I ensure a portfolio "<portfolio>" has created a group called "<group>"
    When I add the user "superuser@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName2>" with description "<envDesc2>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName3>" with description "<envDesc3>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName4>" with description "<envDesc4>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName4>" for app "<appName>" for portfolio "<portfolio>"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName2>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName3>" for feature "<featureKey>"
    And I set the boolean feature value as "true" for environment "<envName4>" for feature "<featureKey>"
    And I ensure the permission "<permission>" is added to the group "<group>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I have a fully registered person "John" with email "John@mailinator.com" and password "password123"
    When I add the user "John@mailinator.com" to the group "<group>" in the portfolio "<portfolio>"

    Examples:
      | appName                 | appDesc           | portfolio    | adminGroup                  | group        | permission   | featureKey     | alias    | featureName             | link                  | envName | envDesc    | envName2 | envDesc2 | envName3 | envDesc3 | envName4 | envDesc4 | valueType |
      | Test app - READ         | FeatureTest1 Desc | Persona test | Persona test Administrators | READ-users   | READ         | FEATURE_SAMPLE | sssshhhh | Sample feature - read   | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | uat      | uat  env | boolean   |
      | Test app - LOCK         | FeatureTest1 Desc | Persona test | Persona test Administrators | LOCK-users   | LOCK         | FEATURE_SAMPLE | sssshhhh | Sample feature - lock   | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | uat      | uat  env | boolean   |
      | Test app - UNLOCK       | FeatureTest1 Desc | Persona test | Persona test Administrators | UNLOCK-users | UNLOCK       | FEATURE_SAMPLE | sssshhhh | Sample feature - unlock | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | uat      | uat  env | boolean   |
      | Test app - CHANGE_VALUE | FeatureTest1 Desc | Persona test | Persona test Administrators | UNLOCK-users | CHANGE_VALUE | FEATURE_SAMPLE | sssshhhh | Sample feature - edit   | http://featurehub.dev | prod    | production | test     | test env | dev      | dev env  | uat      | uat  env | boolean   |


  Scenario Outline: Create apps and add user to 2 groups with same permissions
    Given I ensure a portfolio named "<portfolio>" with description "persona test" exists
    And the first superuser is used for authentication
    When I ensure a portfolio "<portfolio>" has created a group called "<groupA>"
    When I ensure a portfolio "<portfolio>" has created a group called "<groupB>"
    When I add the user "superuser@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "12345" and name "secret1" and link "http://qwe" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    And I ensure all permissions added to the group "<adminGroup>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I ensure all permissions added to the group "<groupA>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure the permission "READ" is added to the group "<groupB>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I have a fully registered person "John" with email "<user>" and password "password123"
    When I add the user "<user>" to the group "<groupA>" in the portfolio "<portfolio>"
    When I add the user "<user>" to the group "<groupB>" in the portfolio "<portfolio>"
    When I can login as user "<user>" with password "password123"
    Then I can get all feature values for this person with a single environment and READ, EDIT, LOCK, UNLOCK permissions

    Examples:
      | appName                     | user                   | appDesc | portfolio    | adminGroup                  | groupA          | groupB    | envName | envDesc    |featureKey|
      | Test app - same permissions | Postman@mailinator.com | Desc123 | Persona test | Persona test Administrators | all permissions | read-only | prod    | production | abc123         |


  Scenario Outline: User is administrator for a portfolio and can read features
    Given I ensure a portfolio named "<portfolio>" with description "business application" exists
    And the first superuser is used for authentication
    When I ensure a portfolio "<portfolio>" has created a group called "<group1>"
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName2>" with description "<envDesc2>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that an environment "<envName3>" with description "<envDesc3>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure all permissions added to the group "<group1>" for the env "<envName>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<group1>" for the env "<envName2>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure all permissions added to the group "<group1>" for the env "<envName3>" for app "<appName>" for portfolio "<portfolio>"
    And I ensure that the feature with the key "<featureKey>" has been removed
    When I create the feature with a key "<featureKey>" and alias "<alias>" and name "<featureName>" and link "<link>" and type "boolean"
    And I can find the feature with a key "<featureKey>"
    When I add the user "superuser@mailinator.com" to the group "<group1>" in the portfolio "<portfolio>"
    And I unlock the feature value for environment "<envName>" for feature "<featureKey>"
    And I unlock the feature value for environment "<envName2>" for feature "<featureKey>"
    And I set the boolean feature value as "true" for environment "<envName>" for feature "<featureKey>"
    And I set the boolean feature value as "false" for environment "<envName2>" for feature "<featureKey>"
    And I ensure the boolean feature value is "true" for environment "<envName>" for feature "<featureKey>"
    And I ensure the boolean feature value is "false" for environment "<envName2>" for feature "<featureKey>"

    # create new user and add to portfolio group - they should be able to read features
    And I have a fully registered person "Sam" with email "sam@mailinator.com" and password "password123"
    When I add the user "sam@mailinator.com" to the group "<adminGroup>" in the portfolio "<portfolio>"

    Examples:
      | appName      | appDesc           | portfolio            | adminGroup                          | group1     | featureKey     | alias         | featureName    | link                           | envName | envDesc    | envName2 | envDesc2 | envName3 | envDesc3 |
      | FeatureTest1 | FeatureTest1 Desc | Portfolio admin ACLs | Portfolio admin ACLs Administrators | developers | FEATURE_SAMPLE | sssshhhh      | Sample feature | http://featurehub.dev          | prod    | production | test     | test env | dev      | dev env  |
      | FeatureTest1 | FeatureTest1 Desc | Portfolio admin ACLs | Portfolio admin ACLs Administrators | developers | NEW_BUTTON     | little_secret | New button     | http://featurehub.dev/new      | prod    | production | test     | test env | dev      | dev env  |
      | FeatureTest1 | FeatureTest1 Desc | Portfolio admin ACLs | Portfolio admin ACLs Administrators | developers | NEW_BOAT       | not_secret    | New boat       | http://featurehub.dev/new/boat | prod    | production | test     | test env | dev      | dev env  |
