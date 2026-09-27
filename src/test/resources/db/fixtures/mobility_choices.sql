-- Mobility choices (needs users.sql and partners.sql).
-- "Y" is the current year, EXTRACT(YEAR FROM CURRENT_DATE) in the session time zone, which the
-- PostgreSQL driver sets to the JVM one: the DAO compares with LocalDate.now().getYear().
--
--   id    user   pref type year term prog country partner denial cancellation  -> expected filter
--   5001  alice  1    SMS  Y    1    1    FR      3001    -      -             -> active
--   5002  alice  2    SMP  Y    2    2    BE      -       -      -             -> active (no partner)
--   5003  alice  3    SMS  Y    1    1    -       -       -      'Changed...'  -> canceled
--   5004  chloe  1    SMS  Y    1    3    CA      3004    4001   -             -> rejected
--   5005  david  1    SMS  Y-2  1    1    FR      3001    -      -             -> passed
--   5006  david  1    SMP  Y    2    1    FR      3001    -      -             -> became a mobility
--   5007  chloe  2    SMS  Y    2    3    CA      3004    -      -             -> became a mobility
--                                                                                  (cancelled, see mobilities.sql)
INSERT INTO student_exchange_tools.denial_reasons (reason_id, reason)
VALUES (4001, 'Dossier incomplet');

INSERT INTO student_exchange_tools.mobility_choices
  (mobility_choice_id, user_id, preference_order, mobility_type, academic_year, term, programme,
   country, submission_date, prof_denial_reason, student_cancellation_reason, partner, version)
VALUES
  (5001, 1001, 1, 'SMS', EXTRACT(YEAR FROM CURRENT_DATE)::int, 1, 1, 'FR', '2025-01-05 10:00:00', NULL, NULL, 3001, 1),
  (5002, 1001, 2, 'SMP', EXTRACT(YEAR FROM CURRENT_DATE)::int, 2, 2, 'BE', '2025-01-05 10:00:01', NULL, NULL, NULL, 1),
  (5003, 1001, 3, 'SMS', EXTRACT(YEAR FROM CURRENT_DATE)::int, 1, 1, NULL, '2025-01-05 10:00:02', NULL, 'Changed my mind', NULL, 2),
  (5004, 1003, 1, 'SMS', EXTRACT(YEAR FROM CURRENT_DATE)::int, 1, 3, 'CA', '2025-01-06 11:30:00', 4001, NULL, 3004, 3),
  (5005, 1004, 1, 'SMS', EXTRACT(YEAR FROM CURRENT_DATE)::int - 2, 1, 1, 'FR', '2023-01-07 08:00:00', NULL, NULL, 3001, 1),
  (5006, 1004, 1, 'SMP', EXTRACT(YEAR FROM CURRENT_DATE)::int, 2, 1, 'FR', '2025-01-08 09:00:00', NULL, NULL, 3001, 1),
  (5007, 1003, 2, 'SMS', EXTRACT(YEAR FROM CURRENT_DATE)::int, 2, 3, 'CA', '2025-01-09 14:00:00', NULL, NULL, 3004, 1);
