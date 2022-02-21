Feature: Meta Data stores and retrieves as expected

  Scenario: I am able to create a feature with metadata
    Given the first superuser is used for authentication
    And I have a randomly named portfolio with the prefix "metadata_port"
    And I create an application with the name "metadata app"
    And I create a feature flag "FEATURE_PARTY"
