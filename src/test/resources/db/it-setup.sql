-- Run once, right after SQLRessources/init.sql, when the integration-test database is created.
--
-- Generated ids start at 100000 so that they can never collide with the explicit ids used by the
-- fixtures in db/fixtures (all below 100000), whatever the order in which the tests run: sequence
-- increments are not undone by the rollback that ends every test.
ALTER SEQUENCE student_exchange_tools.users_user_id_seq RESTART WITH 100000;
ALTER SEQUENCE student_exchange_tools.addresses_address_id_seq RESTART WITH 100000;
ALTER SEQUENCE student_exchange_tools.partners_partner_id_seq RESTART WITH 100000;
ALTER SEQUENCE student_exchange_tools.denial_reasons_reason_id_seq RESTART WITH 100000;
ALTER SEQUENCE student_exchange_tools.mobility_choices_mobility_choice_id_seq RESTART WITH 100000;
ALTER SEQUENCE student_exchange_tools.documents_document_id_seq RESTART WITH 100000;
