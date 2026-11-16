INSERT INTO notification (user_id, type, action, title, message, "read", created_at, updated_at)
SELECT 2, 'APPOINTMENT', 'BOOKED', 'New Appointment Booking',
       'You have a new appointment booking from John Doe on '
           || to_char(CURRENT_DATE + 1, 'YYYY-MM-DD')
           || ' from 10:00 to 10:30.',
       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = 2
      AND title = 'New Appointment Booking'
      AND message = 'You have a new appointment booking from John Doe on '
          || to_char(CURRENT_DATE + 1, 'YYYY-MM-DD')
          || ' from 10:00 to 10:30.'
);

INSERT INTO notification (user_id, type, action, title, message, "read", created_at, updated_at)
SELECT 2, 'APPOINTMENT', 'BOOKED', 'New Appointment Booking',
       'You have a new appointment booking from Jane Clark on '
           || to_char(CURRENT_DATE + 2, 'YYYY-MM-DD')
           || ' from 10:00 to 10:30.',
       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = 2
      AND title = 'New Appointment Booking'
      AND message = 'You have a new appointment booking from Jane Clark on '
          || to_char(CURRENT_DATE + 2, 'YYYY-MM-DD')
          || ' from 10:00 to 10:30.'
);

INSERT INTO notification (user_id, type, action, title, message, "read", created_at, updated_at)
SELECT 3, 'APPOINTMENT', 'BOOKED', 'New Appointment Booking',
       'You have a new appointment booking from John Doe on '
           || to_char(CURRENT_DATE + 3, 'YYYY-MM-DD')
           || ' from 10:00 to 10:15.',
       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = 3
      AND title = 'New Appointment Booking'
      AND message = 'You have a new appointment booking from John Doe on '
          || to_char(CURRENT_DATE + 3, 'YYYY-MM-DD')
          || ' from 10:00 to 10:15.'
);

INSERT INTO notification (user_id, type, action, title, message, "read", created_at, updated_at)
SELECT 3, 'APPOINTMENT', 'BOOKED', 'New Appointment Booking',
       'You have a new appointment booking from Jane Clark on '
           || to_char(CURRENT_DATE + 4, 'YYYY-MM-DD')
           || ' from 13:00 to 13:30.',
       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = 3
      AND title = 'New Appointment Booking'
      AND message = 'You have a new appointment booking from Jane Clark on '
          || to_char(CURRENT_DATE + 4, 'YYYY-MM-DD')
          || ' from 13:00 to 13:30.'
);

INSERT INTO notification (user_id, type, action, title, message, "read", created_at, updated_at)
SELECT 2, 'APPOINTMENT', 'CANCELLED', 'Appointment Cancelled',
       'Appointment cancelled by John Doe for '
           || to_char(CURRENT_DATE - 1, 'YYYY-MM-DD')
           || ' at 11:30.',
       false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification
    WHERE user_id = 2
      AND title = 'Appointment Cancelled'
      AND message = 'Appointment cancelled by John Doe for '
          || to_char(CURRENT_DATE - 1, 'YYYY-MM-DD')
          || ' at 11:30.'
);