@allvariants
Feature: Maintenance banner config is stored in sysconfig and exposed via API

  # ── Scenario 1: initial page load (unauthenticated initialize endpoint) ──────

  @maintenance
  Scenario: Setting maintenance active is visible on the initialize endpoint
    Given I am logged in and have a person configured
    And I enable the maintenance window with message "Scheduled downtime for system upgrades"
    When I call the initialize endpoint
    Then the initialize response has maintenanceInfo active with message "Scheduled downtime for system upgrades"
    When I disable the maintenance window
    And I call the initialize endpoint
    Then the initialize response has no maintenanceInfo

  @maintenance
  Scenario: Maintenance active without a message still sets active flag on initialize
    Given I am logged in and have a person configured
    And I enable the maintenance window without a message
    When I call the initialize endpoint
    Then the initialize response has maintenanceInfo active with no message
    When I disable the maintenance window

  # ── Scenario 2: already logged-in users and in-app navigation ────────────────
  # The /mr-api/maintenance-banner endpoint is what the frontend polls on navigation.
  # It requires no authentication so it works for any session state.

  @maintenance
  Scenario: A logged-in user sees the banner when maintenance is enabled mid-session
    Given I am logged in and have a person configured
    And the maintenance banner endpoint reports no active maintenance
    When I enable the maintenance window with message "We will be back shortly"
    And I navigate to another page
    Then the maintenance banner endpoint reports active maintenance with message "We will be back shortly"

  @maintenance
  Scenario: A logged-in user stops seeing the banner after maintenance ends
    Given I am logged in and have a person configured
    And I enable the maintenance window with message "Brief outage"
    And the maintenance banner endpoint reports active maintenance with message "Brief outage"
    When I disable the maintenance window
    And I navigate to another page
    Then the maintenance banner endpoint reports no active maintenance

  @maintenance
  Scenario: Maintenance active without a message shows on the banner endpoint
    Given I am logged in and have a person configured
    And I enable the maintenance window without a message
    When I navigate to another page
    Then the maintenance banner endpoint reports active maintenance with no message
    When I disable the maintenance window

  @maintenance
  Scenario: Banner endpoint returns no content before any maintenance config has been set
    Given I am logged in and have a person configured
    And I disable the maintenance window
    Then the maintenance banner endpoint reports no active maintenance
