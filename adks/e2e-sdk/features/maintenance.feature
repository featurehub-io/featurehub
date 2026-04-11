Feature: Maintenance banner config is stored in sysconfig and exposed on the initialize endpoint

  @maintenance
  Scenario: Setting maintenance active exposes maintenanceInfo on the initialize endpoint
    Given I am logged in and have a person configured
    And I enable the maintenance window with message "Scheduled downtime for system upgrades"
    When I call the initialize endpoint
    Then the initialize response has maintenanceInfo active with message "Scheduled downtime for system upgrades"
    When I disable the maintenance window
    And I call the initialize endpoint
    Then the initialize response has no maintenanceInfo

  @maintenance
  Scenario: Maintenance active without a message still sets active flag
    Given I am logged in and have a person configured
    And I enable the maintenance window without a message
    When I call the initialize endpoint
    Then the initialize response has maintenanceInfo active with no message
    When I disable the maintenance window
