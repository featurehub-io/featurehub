Feature: This set of scenarios tests the connection when running the party server, or the whole triple
  set of the MR, Dacha and SSE concert. It expects to create a portfolios, service accounts, application, environment
  and features and change their values and see them come through.


#  @EdgeListener @PartyServer
#  Scenario: When I create a fully functioning environment with features, changes come through in a timely fashion
#    Given the first superuser is used for authentication
#    And I have a randomly named portfolio with the prefix "party_server"
#    And I create an application with the name "party server app"
#    And I create an environment "dev"
#    And I create a feature flag "FEATURE_PARTY"
#    And We create a service account "PARTY_ACCOUNT" with the permission READ
#    Then We listen for party server edge features from the shared service account
#    And the feature repository reports the following:
#      | feature       | valueBoolean | locked |
#      | FEATURE_PARTY | false        | true   |
#    And I unlock the feature value for environment "dev" for feature "FEATURE_PARTY"
#    And the feature repository reports the following:
#      | feature       | valueBoolean | locked |
#      | FEATURE_PARTY | false        | false   |


