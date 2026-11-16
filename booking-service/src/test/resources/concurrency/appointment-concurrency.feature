@concurrency
Feature: Concurrent appointment booking

  Scenario: Only one of many simultaneous bookings succeeds
    Given a verified doctor "Sarah Smith" with an open slot next Monday at 10:00
    When 10 different patients book that slot at the same time
    Then exactly 1 booking succeeds
    And the other 9 are rejected with a conflict
