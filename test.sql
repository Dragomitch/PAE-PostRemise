INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des HautVents', '22', 'IE', 'Bruxelles',
        '1080', 'Europe', 1);

INSERT INTO student_exchange_tools.denial_reasons (reason) VALUES ('Je veux plus quitter M. Lehmann & Mr. Grolaux');

INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, is_archived, version)
VALUES (  'Dublin Institute of Technologies Institu', 'DIT',
          'Dublin Institute of Technologies Institu',
          'Institute', 100, 'dit@gmail.com', 'https://dit.com',
          '0444879856', TRUE, 1, FALSE, 1);

INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'prof', 'Dragomir', 'Philippe', 'prof@gmail.com',
   '$2a$10$4eOFXG3pVBe5ZYqeByaQsO/rg/I5I2OIy2zA8UUuyISn5Bd/VpInq', 'Professor', 'BIN', NOW(), 1); -- password : 123456

INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'stud', 'Dragomir', 'Philippe', 'stud@gmail.com',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Student', 'BIN', NOW(), 1); -- password : 123456

INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (2, 1, 'SMS', 2015, 1, 1, 'BE', NOW(), NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobilities(mobility_choice_id, submission_date, state, state_before_cancellation,
                                              first_payment_request_date, second_payment_request_date, pro_eco_encoding,
                                              second_software_encoding, student_cancellation_reason, prof_denial_reason,
                                              professor_in_charge, version)
VALUES (1, NOW(), 'Créée', NULL, now(), NULL, FALSE, FALSE, NULL, NULL, 1, 1) RETURNING mobility_choice_id;

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (1, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (2, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (3, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (4, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (5, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (14, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (15, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (16, 1, FALSE, 1);
INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version) VALUES (17, 1, FALSE, 1);

SELECT * FROM student_exchange_tools.partners;
