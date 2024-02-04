@allvariants @streamingvariants
Feature: All flag based functionality works as expected

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments
    And I connect to the feature server

    @retired @history
  Scenario: A new portfolio with a boolean feature is retired and no longer exists
    Given There is a new feature flag
    Then the feature flag is locked and off
    When I retire the feature flag
    Then there are 0 features
    And I set the feature flag to on and unlocked
    Then there are 0 features
    When I unretire the feature flag
    Then there are 1 features
    Then the feature flag is unlocked and on
    When I check the feature history I see
      | locked | retired | value |
      | true   | false   | off   |
      | false  | true    | off   |
      | false  | true    | on    |
      | false  | false   | on    |

  @flag-lock
  Scenario: A locked flag cannot have the strategy changed
    Given There is a new feature flag
    Then the feature flag is locked and off
    And I cannot create custom flag rollout strategies
      | percentage | name          | value  |
      | 15         | orange-roughy | true |


  @history2
   Scenario: A new portfolio with complex history
     Given There is a new feature flag
     And I set the feature flag to on and unlocked
     And I create custom rollout strategies
       | percentage | name          | value  |
       | 15         | orange-roughy | on |
       | 12         | green-diamon  | off  |
     And I create custom rollout strategies
       | percentage | name          | value  |
       | 25         | orange-roughy | on |
       | 16         | green-diamon  | on  |
       | 50         | blue-peter    | off   |
     When I check the feature history I see
       | locked | retired | value | strategies                                               |
       | true   | false   | off   |                                                          |
       | false  | false   | on    | 15/orange-roughy/on,12/green-diamon/off                  |
       | false  | false   | on    | 25/orange-roughy/on,16/green-diamon/on,50/blue-peter/off |


    # assumes server is configured with
    # sdk.feature.properties=appName={{{feature.parentApplication.name}}},portfolio={{{feature.parentApplication.portfolio.name}}},category={{{metadata.category}}}
  @extended-data
  Scenario: When an API key is allowed extended data access, we will get enriched feature properties
    Given There is a new feature flag
    Then there is no enriched data
    When we update the metadata to include '{"category":"shoes"}'
    Then there is no enriched data
    When we allow the service account access to the enriched data
    And I sleep for 5 seconds
    And I bounce the feature server connection
    Then there is enriched data
      | field     | value            |
      | portfolio | portfolio.name   |
      | appName   | application.name |
      | category  | shoes            |
    When we update the metadata to include '{"kebabFlavour":"lamb"}'
    Then there is enriched data
      | field | value |
      | portfolio | portfolio.name |
      | appName   | application.name |


  @flags
  Scenario: A new portfolio with a boolean feature
#    Given I connect to the Edge server using <ConnectionType>
    Given There is a new feature flag
    Then the feature flag is locked and off
    When I unlock the feature
    Then the feature flag is unlocked and off
    And I set the feature flag to on
    Then the feature flag is unlocked and on
    And I set the feature flag to off
    And I lock the feature
    Then the feature flag is locked and off
    And I unlock the feature
    Then I add a strategy X with no percentage and value on
      | Field   | Type   | Conditional | Values               |
      | userkey | STRING | EQUALS      | user1@mailinator.com |
    Then the feature flag is unlocked and off
    Then I set the context to
      | Field   | Value                |
      | userkey | user1@mailinator.com |
    And the feature flag is unlocked and on
    Then I set the context to
      | Field   | Value                |
      | userkey | user2@mailinator.com |
    And the feature flag is unlocked and off
    And I clear the context
#    Examples:
#      | ConnectionType   |
#      | sse-client-eval  |
#      | poll-client-eval |
#      | poll-server-eval |

