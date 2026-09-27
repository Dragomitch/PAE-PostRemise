-- Addresses and partners (needs nothing but init.sql).
--   Addresses: 2001 FR (region set), 2002 BE (region NULL), 2003 CA, 2004 FR
--   Partners:
--     3001 "Université de Lyon"        FR official     active    options BIN, BCH
--     3002 "Archived Paris School"     FR official     archived  option  BIN
--     3003 "Unofficial Brussels Lab"   BE not official active    option  BIN
--     3004 "Montreal Polytechnic"      CA official     active    option  BCH
--     3005 "Orphan Partner"            FR official     active    no option at all
INSERT INTO student_exchange_tools.addresses
  (address_id, street, number, country, city, postal_code, region, version)
VALUES
  (2001, 'Rue de la République', '12B', 'FR', 'Lyon', '69002', 'Auvergne-Rhône-Alpes', 1),
  (2002, 'Rue Royale', '1', 'BE', 'Bruxelles', '1000', NULL, 4),
  (2003, 'Rue Sherbrooke', '2900', 'CA', 'Montréal', 'H3T 1J4', 'Québec', 1),
  (2004, 'Boulevard Saint-Michel', '60', 'FR', 'Paris', '75006', NULL, 1);

INSERT INTO student_exchange_tools.partners
  (partner_id, legal_name, business_name, full_name, organisation_type, employee_count, address,
   email, website, phone_number, is_official, is_archived, version)
VALUES
  (3001, 'Université Lyon SA', 'UdL', 'Université de Lyon', 'University', 3000, 2001,
   'contact@lyon.test', 'https://lyon.test', '+33400000001', TRUE, FALSE, 2),
  (3002, 'Paris School SA', 'PS', 'Archived Paris School', 'School', 120, 2004,
   'contact@paris.test', 'https://paris.test', '+33100000002', TRUE, TRUE, 1),
  (3003, 'Brussels Lab SPRL', 'BLab', 'Unofficial Brussels Lab', 'SME', 12, 2002,
   'contact@blab.test', 'https://blab.test', '+3220000003', FALSE, FALSE, 1),
  (3004, 'Polytechnique Montréal', 'PolyMtl', 'Montreal Polytechnic', 'University', 1500, 2003,
   'contact@poly.test', 'https://poly.test', '+15140000004', TRUE, FALSE, 1),
  (3005, 'Orphan SA', 'Orphan', 'Orphan Partner', 'SME', 5, 2001,
   'contact@orphan.test', 'https://orphan.test', '+33400000005', TRUE, FALSE, 1);

INSERT INTO student_exchange_tools.partner_options (option_code, partner_id, departement)
VALUES
  ('BIN', 3001, 'Informatique'),
  ('BCH', 3001, 'Chimie'),
  ('BIN', 3002, 'Informatique'),
  ('BIN', 3003, 'Data'),
  ('BCH', 3004, 'Génie chimique');
