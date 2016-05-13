START TRANSACTION;
-- Inserts users 'Professor'
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Lehmann', 'Lehmann', 'Brigitte', 'donatien.grolaux@vinci.be',
   '$2a$10$gsmAFrOcJzIAcjxvJYiM7.Wk1I.MhCMUf9.bThOzk4lUnFdKAbc/C', 'Professor', 'BIN', NOW(), 1); -- password : Lehmann

-- Inserts users 'Student'
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Dimov', 'Dimov', 'Theodor', 'dimov.theodor@gmail.com',
   '$2a$10$oL2tP2vduHJs1i5NgMDGru24APymi21OiYxag2pOdsLfSIRgrzYSK', 'Student', 'BDI', NOW(), 1); -- password : Dimov

INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Haut Vents', '22', 'BE', 'Bruxelles', '1080', '', 1);

INSERT INTO student_exchange_tools.nominated_students (user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (2, 'mr', now(), 1, 'BE', '+32323244334', 'm', 2, 'BE9827423432423', NULL, 'Bnp Paribas Fortis', 'GEBABBEB', 1);
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Dragomir', 'Dragomir', 'Philippe', 'dragomir.philippe@gmail.com',
   '$2a$10$UDUBTRk6qkN7gMTLrfjJEeEJLMdcpIBfcqblk4qn2G9lqf9owM4.6', 'Student', 'BIN', NOW(), 1); -- password : Dragomir

INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Pimprenelles', '47', 'BE', 'Bruxelles', '1080', '', 1);

INSERT INTO student_exchange_tools.nominated_students (user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (3, 'mr', now(), 2, 'BE', '+32491736687', 'm', 2, 'BE9827423432423', NULL, 'Bnp Paribas Fortis', 'GEBABBEB', 1);

--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Oste', 'Oste', 'Nicolas', 'oste.nicolas@gmail.com',
   '$2a$10$bbHGSuaF.vUaPNL6mxpn7O5u5CAmW6abFMZESpqOO9kS4P6OF2U0W', 'Student', 'BDI', NOW(), 1); -- password : Oste

INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Hirondelles', '222', 'BE', 'Etterbeek', '1040', '', 1);

INSERT INTO student_exchange_tools.nominated_students (user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (4, 'mr', now(), 3, 'BE', '+32491736687', 'm', 2, 'BE9827423432423', NULL, 'Bnp Paribas Fortis', 'GEBABBEB', 1);
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Turing', 'Turing', 'Alan', 'turing@creators.com',
   '$2a$10$TxGjLIrUwzh4/b5p/P1ZFeVcNMc.jIilWOatC5WchMW1SuZyA2b/q', 'Student', 'BIN', NOW(), 1); -- password : Turing

INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Hiboux', '147', 'BE', 'Etterbeek', '1040', '', 1);

INSERT INTO student_exchange_tools.nominated_students (user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (5, 'mr', now(), 4, 'BE', '+32491736687', 'm', 2, 'BE9827423432423', NULL, 'Bnp Paribas Fortis', 'GEBABBEB', 1);
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'Wagemans', 'Wagemans', 'Jeremy', 'wagemans.jeremy@me.com',
   '$2a$10$eX0TpxAzoRHE7.cTRO/zyegzLhVYYVsmSDzsvFBU.rutlP36KCFEu', 'Student', 'BIN', NOW(), 1); -- password : Wagemans

INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Gouroux', '666', 'BE', 'Woluwe-Saint-Lambert', '1200', '', 1);

INSERT INTO student_exchange_tools.nominated_students (user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (6, 'mr', now(), 5, 'BE', '+32491736687', 'm', 2, 'BE9827423432423', NULL, 'Bnp Paribas Fortis', 'GEBABBEB', 1);
--

-- Inserts partners
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'DIT International Office, Rathmines Road, Rathmines', '143-149', 'IE', 'Dublin', 'Dublin 6', '', 1);
INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, departement, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, version)
VALUES ('Dublin Institute of Technology', 'Dublin Institute of Technology',
                                          'Dublin Institute of Technology', 'School of Computing',
                                          'TGE', 2000, 'peter.dalton@dit.ie', 'http://www.dit.ie', '', TRUE, 6, 1);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BIN', 1);
--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, '', '', 'BE', 'Leuven', '3000', 'Vlaams Brabant', 1);
INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, departement, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, version)
VALUES ('University College Leuven Limburg', 'University College Leuven Limburg',
                                             'University College Leuven Limburg', 'departement',
                                             'TGE', 1984, 'Griet.tservranckx@ucll.be', 'http://www.ucll.be', '', TRUE,
                                             7, 1);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BIN', 2);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BDI', 2);
--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des affaires', '10', 'LU', 'Jamoigne', '6810', 'Luxembourg', 1);

INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, departement, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, version)
VALUES ('AferDanLSac', 'AferDanLSac',
                       'AferDanLSac', 'Une ardeur davance',
                       'TGE', 1984, 'AferDanLSac@affaires.be', '', '', TRUE, 8, 1);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BIN', 3);

--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Chemin de Chambly', '945', 'CA', 'Longueil', 'QC J4H 3M6', 'Canada', 1);

INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, departement, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, version)
VALUES ('Cégep Édouard Montpetit', 'Cégep Édouard Montpetit',
                                   'Cégep Édouard Montpetit', 'Science de la santé',
                                   'TGE', 1984, 'AferDanLSac@affaires.be', '', '', TRUE, 9, 1);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BDI', 4);

--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Princeton', '10', 'US', 'New Jersey', 'NJ 08544', 'United States', 1);

INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, departement, organisation_type,
                                             employee_count, email, website, phone_number, is_official, address, version)
VALUES ('Princeton University', 'Princeton University',
                                'Princeton University', 'Informatique',
                                'TGE', 1984, 'princetonuniv@princeton.edu', '', '', TRUE, 10, 1);
INSERT INTO student_exchange_tools.partner_options
VALUES ('BIN', 5);

-- Inserts mobility choices
-- Dimov
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner, version)
VALUES (2, 1, 'SMS', 2016, 1, 2, 'BE', NOW(), NULL, NULL, 2, 1);

-- Dragomir
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner, version)
VALUES (3, 1, 'SMS', 2016, 1, 1, 'LU', NOW(), NULL, NULL, 3, 1);

-- Oste
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner, version)
VALUES (4, 1, 'SMS', 2016, 1, 3, 'CA', NOW(), NULL, NULL, 4, 1);

-- Turing
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner, version)
VALUES (5, 1, 'SMS', 2016, 2, 3, 'US', NOW(), NULL, NULL, 5, 1);

-- Wagemans
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner, version)
VALUES (6, 1, 'SMS', 2016, 2, 1, 'IE', NOW(), NULL, NULL, 1, 1);

-- Insert Mobilities
--Dimov
INSERT INTO student_exchange_tools.mobilities (mobility_choice_id ,submission_date, state, state_before_cancellation,
                                               first_payment_request_date, second_payment_request_date,
                                               pro_eco_encoding, second_software_encoding, student_cancellation_reason,
                                               prof_denial_reason, professor_in_charge, version)
VALUES (1 ,now(), 'Créée', NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (6, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (7, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (8, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (9, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (18, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (19, 1, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (20, 1, FALSE, 1);

--Dragomir
INSERT INTO student_exchange_tools.mobilities (mobility_choice_id ,submission_date, state, state_before_cancellation,
                                               first_payment_request_date, second_payment_request_date,
                                               pro_eco_encoding, second_software_encoding, student_cancellation_reason,
                                               prof_denial_reason, professor_in_charge, version)
VALUES (2 ,now(), 'Créée', NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (1, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (2, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (3, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (4, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (5, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (14, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (15, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (16, 2, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (17, 2, FALSE, 1);

--Oste
INSERT INTO student_exchange_tools.mobilities (mobility_choice_id ,submission_date, state, state_before_cancellation,
                                               first_payment_request_date, second_payment_request_date,
                                               pro_eco_encoding, second_software_encoding, student_cancellation_reason,
                                               prof_denial_reason, professor_in_charge, version)
VALUES (3 ,now(), 'Créée', NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (10, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (11, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (12, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (13, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (21, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (22, 3, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (23, 3, FALSE, 1);

--Turing
INSERT INTO student_exchange_tools.denial_reasons (reason)
VALUES ('Demande non réalisable');

INSERT INTO student_exchange_tools.mobilities (mobility_choice_id ,submission_date, state, state_before_cancellation,
                                               first_payment_request_date, second_payment_request_date,
                                               pro_eco_encoding, second_software_encoding, student_cancellation_reason,
                                               prof_denial_reason, professor_in_charge, version)
VALUES (4 ,now(), 'Annulée', NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (10, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (11, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (12, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (13, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (21, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (22, 4, FALSE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (23, 4, FALSE, 1);

--Wagemans
INSERT INTO student_exchange_tools.mobilities (mobility_choice_id ,submission_date, state, state_before_cancellation,
                                               first_payment_request_date, second_payment_request_date,
                                               pro_eco_encoding, second_software_encoding, student_cancellation_reason,
                                               prof_denial_reason, professor_in_charge, version)
VALUES (5 ,now(), 'Terminée', NULL, NULL, NULL, FALSE, FALSE, NULL, NULL, 1, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (1, 5, TRUE , 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (2, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (3, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (4, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (5, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (14, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (15, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (16, 5, TRUE, 1);

INSERT INTO student_exchange_tools.mobility_documents (document_id, mobility_id, is_filled_in, version)
VALUES (17, 5, TRUE, 1);
COMMIT;