Feature: Appointment booking

  Background:
    Given a verified doctor "Sarah Smith" with an open slot next Monday at 10:00
    And a registered patient "John Doe"

  Scenario: A patient books an open slot
    When "John Doe" books that slot
    Then the booking is confirmed for "Sarah Smith"
    And the slot is no longer available

  Scenario: The same slot cannot be booked twice
    Given "John Doe" has booked that slot
    When "Jane Clark" books that slot
    Then the booking is rejected with a conflict

  Scenario: A patient cancels a booking and the slot opens again
    Given "John Doe" has booked that slot
    When "John Doe" cancels that booking
    Then the appointment is cancelled
    And the slot is available again
