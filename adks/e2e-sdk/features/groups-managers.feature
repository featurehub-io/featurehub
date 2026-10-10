@allvariants @streamingvariants
Feature: Group managers

  Background:
    Given I create a new portfolio
    And I create an application
    And I create a service account and full permissions based on the application environments

    @gmm @gmmpermission
  Scenario: A person can be added to a group manager group and add and remove people from any group
    Given I create a new user "GMM1"
    And I create a new user "GMM2"
    And I create a new group "GMM"
    And I can assign portfolio strategy "manage" roles to the group
    And I add the user "GMM1" to the group "GMM"
    And I add the user "GMM2" to the group "GMM"
    And I create a new group "ACL"
    And I create a new user "ACL"
    And I add the user "ACL" to the group "ACL"
    And I can assign roles "all" to the group for the current environment
    And I cannot add the user "GMM1" to the group "ACL"
    When I am the user "GMM1"
    Then I can remove the user "ACL" from the group "ACL"
    And I cannot add the user "GMM1" to the group "ACL"
    And I cannot add the user "GMM2" to the group "ACL"
    And I can add the user "ACL" to the portfolio admin group
    And I cannot add the user "GMM1" to the portfolio admin group
    When I am the superuser
    And I cannot add the user "GMM1" to the portfolio admin group

      @gmmroles
  Scenario: A group manager group cannot have other portfolio roles
    Given I create a new user "GMM1"
    And I create a new group "GMM"
    And I can assign portfolio strategy "manage" roles to the group
    And I add the user "GMM1" to the group "GMM"
    And I cannot assign portfolio strategy "manage,edit" roles to the group
