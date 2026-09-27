-- Nominated students (needs users.sql and partners.sql for the addresses).
--   1001 alice  Ms  born 2001-04-12, Belgian, address 2001, no card holder, version 1
--   1004 david  Mr  born 1999-12-31, French,  address 2004, card holder set, version 2
INSERT INTO student_exchange_tools.nominated_students
  (user_id, title, birthdate, nationality, address, phone_number, gender, passed_years_count, iban,
   card_holder, bank_name, bic, version)
VALUES
  (1001, 'Ms', '2001-04-12 00:00:00', 'BE', 2001, '+32470000001', 'F', 2,
   'BE68539007547034', NULL, 'Belfius', 'GKCCBEBB', 1),
  (1004, 'Mr', '1999-12-31 00:00:00', 'FR', 2004, '+33600000004', 'M', 3,
   'FR1420041010050500013M02606', 'David Petit', 'La Banque Postale', 'PSSTFRPPXXX', 2);
