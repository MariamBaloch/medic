Feature: Booking service access

  Scenario: A patient must sign in to see appointments
    When a client lists patient appointments without a token
    Then the response status is 401

  Scenario: The health endpoint reports the service is up
    When a client calls the health endpoint without a token
    Then the response status is 200
    And the health status is "UP"
