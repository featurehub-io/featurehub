Feature: This set of scenarios tests the connection when running the party server, or the whole triple



  @EdgeListener @PartyServer
  Scenario: When I create a fully functioning environment with features, changes come through in a timely fashion
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "party_server"
    And I create an application with the name "party server app"
    And I create an environment "dev"
    And I create a feature flag "FEATURE_PARTY"
    And We create a service account "PARTY_ACCOUNT" with the permission READ
    And I wait for 2 seconds
    Then We listen for party server edge features from the shared service account
    And the feature repository reports the following:
      | feature       | valueBoolean |
      | FEATURE_PARTY | false        |
    And I unlock the feature value for environment "dev" for feature "FEATURE_PARTY"
    And I set the boolean feature value as "true" for environment "dev" for feature "FEATURE_PARTY"
    And I wait for 1 seconds
    And the feature repository reports the following:
      | feature       | valueBoolean |
      | FEATURE_PARTY | true         |


  Scenario: When I create a fully functioning environment and use the GET API with OPTIONS checks, changes come through as expected
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "party_server"
    And I create an application with the name "party server GET test"
    And I create an environment "dev"
    And I create a feature flag "FEATURE_PARTY"
    And We create a service account "PARTY_ACCOUNT" with the permission READ
    And I wait for 2 seconds
    Then we do an OPTIONS check on the API to ensure we have CORS access
      | header                           | valueContains                                                                                         |
      | Access-Control-Allow-Credentials | true                                                                                                  |
      | Access-Control-Allow-Methods     | GET, POST, PUT, DELETE, OPTIONS, HEAD |
      | Access-Control-Allow-Headers     | X-Requested-With,Authorization,Content-type,Accept-Version,Content-MD5,CSRF-Token,x-ijt,cache-control,x-featurehub,Baggage |
      | Access-Control-Allow-Origin      | *                                                                                                     |
    And we poll the current service account to full the repository
    And the feature repository reports the following:
      | feature       | valueBoolean |
      | FEATURE_PARTY | false        |
    And I unlock the feature value for environment "dev" for feature "FEATURE_PARTY"
    And I set the boolean feature value as "true" for environment "dev" for feature "FEATURE_PARTY"
    And I wait for 1 seconds
    And we poll the current service account to full the repository
    And the feature repository reports the following:
      | feature       | valueBoolean |
      | FEATURE_PARTY | true         |
