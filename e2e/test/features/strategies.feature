Feature: Strategies work as expected


  Scenario Outline: A feature is able to store shared rollout strategy percentages
    Given I ensure a portfolio named "<portfolio>" with description "business application" exists
    And the first superuser is used for authentication
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that the string feature with the key <featureKey> exists and has the default value <defaultValue>
    And I create shared rollout strategies
      | percentage | name          |
      | 32         | blue-kachoo   |
      | 45         | yellow-smello |
    And I apply the rollout strategies to the current feature value
      | name          | value |
      | blue-kachoo   | blue  |
      | yellow-smello | yellow |
    And I confirm on getting the feature it has the same data as set

    Examples:
      | appName      | appDesc           | portfolio               | featureKey          | envName | envDesc    | defaultValue |
      | StrategyTest | StrategyTest Desc | Strategy Test Portfolio | SUBMIT_COLOR_BUTTON | prod    | production | orange       |

  Scenario Outline: A feature is able to store custom rollout strategy percentages
    Given I ensure a portfolio named "<portfolio>" with description "business application" exists
    And the first superuser is used for authentication
    Given I ensure an application with the name "<appName>" with description "<appDesc>" in the portfolio "<portfolio>" exists
    And I ensure that an environment "<envName>" with description "<envDesc>" exists in the app "<appName>" in the portfolio "<portfolio>"
    And I ensure that the string feature with the key <featureKey> exists and has the default value <defaultValue>
    And I create custom rollout strategies
      | percentage | name          | value  |
      | 15         | orange-roughy | orange |
      | 12         | green-diamon  | green  |
    And I confirm on getting the feature value has the custom rollout strategies set

    Examples:
      | appName      | appDesc           | portfolio               | featureKey          | envName | envDesc    | defaultValue |
      | StrategyTest | StrategyTest Desc | Strategy Test Portfolio | SUBMIT_COLOR_BUTTON | prod    | production | purple       |


