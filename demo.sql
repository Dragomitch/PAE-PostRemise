START TRANSACTION;
-- Inserts users 'Professor'
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'grolaux', 'Grolaux', 'Donatien', 'donatien.grolaux@vinci.be',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Professor', 'BIN', NOW(), 1); -- password : 123456
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'lehmann', 'Lehmann', 'Brigitte', 'brigitte.lehmann@vinci.be',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Professor', 'BIN', NOW(), 1); -- password : 123456

-- Inserts users 'Student'
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'pierre', 'Kiroule', 'Pierre', 'pierre.kiroule@gmail.com',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Student', 'BDI', NOW(), 1); -- password : 123456
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'namasse', 'Pamousse', 'Namasse', 'namasse.pamousse@gmail.com',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Student', 'BIN', NOW(), 1); -- password : 123456
--
INSERT INTO student_exchange_tools.users VALUES
  (DEFAULT, 'tonthe', 'Tatilotetatou', 'Tonthe', 'thonthe.tatilotetatou@gmail.com',
   '$2a$10$CvyebLjjPeK7fh8z/IwTkekuYLrYXlhEKa2zcNeDhcKAmXuL2SMb6', 'Student', 'BDI', NOW(), 1); -- password : 123456
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue des Haut Vents', '22', 'BE', 'Bruxelles', '1080', '', 1);
INSERT INTO student_exchange_tools.nominated_students(user_id, title, birthdate, address, nationality, phone_number, gender, passed_years_count, iban, card_holder, bank_name, bic, version)
VALUES (5, 'Mr', '1994-03-18', 1, 'BE', '+32323244334', 'M', 2, 'BE9827423432423', null, 'Bnp Paribas Fortis', 'GEBABBEB', 1);

-- Inserts partners
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'DIT International Office, Rathmines Road, Rathmines', '143-149', 'IE', 'Dublin', 'Dublin 6', '', 1);
INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, organisation_type,
                                             employee_count, email, website, phone_number, is_archived, is_official, address, version)
VALUES ('Dublin Institute of Technology', 'Dublin Institute of Technology',
          'Dublin Institute of Technology',
          'TGE', 2000, 'peter.dalton@dit.ie', 'http://www.dit.ie', '+39456732364', FALSE, FALSE, 2, 1);
INSERT INTO student_exchange_tools.partner_options
    VALUES ('BIN', 1, 'School of Computing');
--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Blublustraat', '24', 'BE', 'Leuven', '3000', 'Vlaams Brabant', 1);
INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, organisation_type,
                                             employee_count, email, website, phone_number, is_archived, is_official, address, version)
VALUES ('University College Leuven Limburg', 'University College Leuven Limburg',
          'University College Leuven Limburg',
          'TGE', 1984, 'Griet.tservranckx@ucll.be', 'http://www.ucll.be', '+32456732364', FALSE, TRUE, 3, 1);
INSERT INTO student_exchange_tools.partner_options
    VALUES ('BIN', 2, 'Applied Information Technology');
INSERT INTO student_exchange_tools.partner_options
    VALUES ('BDI', 2, 'Health & Wellfare');
--
INSERT INTO student_exchange_tools.addresses
VALUES (DEFAULT, 'Rue du Faing', '10', 'LU', 'Jamoigne', '6810', 'Luxembourg', 1);
INSERT INTO student_exchange_tools.partners (legal_name, business_name, full_name, organisation_type,
                                             employee_count, email, website, phone_number, is_archived, is_official, address, version)
VALUES ('Université du Sanglier', 'Université du Sanglier',
          'Université du Sanglier',
          'TGE', 1984, 'jamoigne@lux.be', 'www.lux.com', '+32456732364', FALSE, TRUE, 4, 1);
INSERT INTO student_exchange_tools.partner_options
    VALUES ('BIN', 3, 'Une ardeur d''avance');

-- Inserts mobility choices
-- Pierre Kiroule
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (3, 1, 'SMS', 2016, 1, 2, 'BE', NOW(), NULL, NULL, 2, 1);
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (3, 1, 'SMP', 2016, 2, 2, 'BE', NOW(), NULL, NULL, NULL, 1);
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (3, 2, 'SMP', 2016, 2, 1, 'CH', NOW(), NULL, NULL, NULL, 1);
-- Namasse Pamousse
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (4, 1, 'SMS', 2016, 1, 1, 'BE', NOW(), NULL, NULL, 1, 1);
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (4, 2, 'SMS', 2016, 1, 2, 'BE', NOW(), NULL, NULL, 2, 1);
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (4, 3, 'SMP', 2016, 2, 1, 'GB', NOW(), NULL, NULL, NULL, 1);
-- Tonthe Tatilotetatou
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (5, 1, 'SMS', 2016, 1, 2, 'IE', NOW(), NULL, NULL, 2, 1);
INSERT INTO student_exchange_tools.mobility_choices (user_id, preference_order, mobility_type, academic_year, term,
                                                     programme, country, submission_date, prof_denial_reason,
                                                     student_cancellation_reason, partner,version)
VALUES (5, 1, 'SMP', 2016, 2, 1, NULL, NOW(), NULL, NULL, NULL, 1);
COMMIT;