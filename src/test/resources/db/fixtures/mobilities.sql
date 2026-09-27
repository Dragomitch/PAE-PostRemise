-- Mobilities and their documents (needs users.sql, partners.sql and mobility_choices.sql).
--   5006 david -> Université de Lyon (FR, Erasmus+), "En préparation", professor bob (1002),
--        departure payment requested on 2025-02-01 09:00, version 2
--        documents: 1 (D, filled in), 2 (D, not filled in), 14 (R, not filled in)
--   5007 chloe -> Montreal Polytechnic (CA, FAME), "Annulée" (was "Créée"), both payments
--        requested, cancelled by the student AND denied by a professor (reason 4001), no professor
INSERT INTO student_exchange_tools.mobilities
  (mobility_choice_id, submission_date, state, state_before_cancellation, first_payment_request_date,
   second_payment_request_date, pro_eco_encoding, second_software_encoding,
   student_cancellation_reason, prof_denial_reason, professor_in_charge, version)
VALUES
  (5006, '2025-01-10 10:00:00', 'En préparation', NULL, '2025-02-01 09:00:00', NULL, TRUE, FALSE,
   NULL, NULL, 1002, 2),
  (5007, '2025-01-11 11:00:00', 'Annulée', 'Créée', '2025-03-01 08:00:00', '2025-06-30 16:30:00',
   FALSE, TRUE, 'Sick', 4001, NULL, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES
  (1, 5006, TRUE, 2),
  (2, 5006, FALSE, 1),
  (14, 5006, FALSE, 1);
