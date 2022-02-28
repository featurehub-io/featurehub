Feature: Meta Data stores and retrieves as expected

  Scenario: I am able to create a feature with metadata
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "metadata_port"
    And I create an application with the name "metadata app"
    When I create a feature flag "FEATURE_PARTY" with a description "this is the description" and meta-data "some metadata"
    Then the feature "FEATURE_PARTY" has the description "this is the description"
    Then the feature "FEATURE_PARTY" has the metadata "some metadata"
    And I ensure that the string feature with the key FEATURE_MARY exists and has the default value zoot
    And I set the feature FEATURE_MARY description to "mary description"
    And I set the feature FEATURE_MARY metadata to "mary metadata"
    Then the feature "FEATURE_MARY" has the description "mary description"
    Then the feature "FEATURE_MARY" has the metadata "mary metadata"
