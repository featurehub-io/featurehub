@needs-webserver @allvariants @streamingvariants
Feature: System supports Slack

  @slack-external @slack @notsaas
  Scenario: I setup and test a slack event to an external app
    Given I am logged in and have a person configured
    And I create a new portfolio
    And I create an application
    And I update the system config for Slack delivery to external source
    And I update the environment for Slack
    And I wait for 5 seconds
    And I clear cloud events
    When There is a feature flag with the key FEATURE_TITLE_TO_UPPERCASE
    And I wait for 5 seconds
    Then I receive a create "integration/slack-v1" cloud event
      | locked | retired | value |
      | true   | false   | false |
    And I clear cloud events
      # we have to set it now otherwise it won't trigger
    And I set the feature flag to unlocked and on
    Then I receive a cloud event of type "integration/slack-v1"
      | previous-or-next | locked | retired | value |
      | p                | true   | false   | false |
      | n                | false  | false   | true  |
    And I clear cloud events
    And I create portfolio strategies
      | name   | fieldName | conditional | values | type   |
      | chubby | customer  | equals      | ilf    | string |
    And I attach portfolio strategy "chubby" to the current environment feature value with the value "pearl"
    Then I receive a cloud event of type "integration/slack-v1"
      | previous-or-next | portfolio-strategy-name | portfolio-strategy-fieldName | portfolio-strategy-conditional | portfolio-strategy-values | portfolio-strategy-type | portfolio-strategy-value |
      | a                | chubby                  | customer                     | equals                         | ilf                       | string                  | pearl                    |

  @slack-direct  @slack  @notsaas
  Scenario: I setup and test a slack event directly to slack
    Given I am logged in and have a person configured
    And I create a new portfolio
    And I create an application
    And I update the system config for Slack delivery direct to Slack
    And I redirect slack.com traffic to this testing service
    And I update the environment for Slack
    And I wait for 5 seconds
    And I clear slack events
    When There is a feature flag with the key keiko
    Then I receive a slack message containing the key "keiko"
    Then I receive a slack message containing the key "created"
    # we have to set it now otherwise it won't trigger
    And I set the feature flag to unlocked and on
    Then I receive a slack message containing the key "keiko"
    Then I receive a slack message containing the key "changed"
    And I create a feature group "saas-group"
      | key    | value |
      | keiko  | true |
    And I clear slack events
    When I update the strategy in the feature group
      | name    | percentage | percentageAttributes | fieldName | conditional | values   | type   |
      | country | _          | _                    | country   | ne          | thailand | string |
    Then I receive a slack message containing the key "keiko"
    Then I receive a slack message containing the key "Added feature group strategies"

