-- Users (reference data only: options BIN/BCH come from init.sql).
--   1001 alice  Student   BIN  version 1
--   1002 bob    Professor BIN  version 3
--   1003 chloe  Student   BCH  version 1
--   1004 david  Student   BIN  version 2
INSERT INTO student_exchange_tools.users
  (user_id, username, last_name, first_name, email, password, role, option, registration_date, version)
VALUES
  (1001, 'alice', 'Martin', 'Alice', 'alice@student.test', 'hash-alice', 'Student', 'BIN', '2024-09-01 08:30:00', 1),
  (1002, 'bob', 'Dupont', 'Bob', 'bob@prof.test', 'hash-bob', 'Professor', 'BIN', '2020-01-15 10:00:00', 3),
  (1003, 'chloe', 'Leroy', 'Chloé', 'chloe@student.test', 'hash-chloe', 'Student', 'BCH', '2024-09-02 09:15:30', 1),
  (1004, 'david', 'Petit', 'David', 'david@student.test', 'hash-david', 'Student', 'BIN', '2024-09-03 17:45:00', 2);
