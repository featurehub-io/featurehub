Feature: I am testing initialization scenarios

  @superuser
  Scenario Outline: A newly initialized system with a portfolio must be able to create a normal group
    And The superuser is the user
    When I ensure a portfolio "<portfolio>" has created a group called "<group>"
    Then portfolio "<portfolio>" has group "<group>"

    Examples:
      | portfolio        | group             |
      | Sample Portfolio | Sample Port Group |
