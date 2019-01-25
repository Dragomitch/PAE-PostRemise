-- Removes SQL schema
DROP SCHEMA IF EXISTS student_exchange_tools CASCADE;
 
-- Creates Schema
CREATE SCHEMA student_exchange_tools;
 
-- Creates tables
CREATE TABLE student_exchange_tools.options (
  option_code CHAR(3) PRIMARY KEY,
  name        VARCHAR(50) NOT NULL
);
 
CREATE TABLE student_exchange_tools.users (
  user_id           SERIAL PRIMARY KEY,
  username          VARCHAR(20)  NOT NULL,
  last_name         VARCHAR(35)  NOT NULL,
  first_name        VARCHAR(35)  NOT NULL,
  email             VARCHAR(255) NOT NULL,
  password          VARCHAR(255) NOT NULL,
  role              VARCHAR(15)  NOT NULL,
  option            CHAR(3)      NOT NULL
    REFERENCES student_exchange_tools.options (option_code),
  registration_date TIMESTAMP    NOT NULL,
  version           INTEGER      NOT NULL
);

CREATE TABLE student_exchange_tools.programmes (
  programme_id           SERIAL PRIMARY KEY,
  name                   VARCHAR(100) NOT NULL,
  external_software_name VARCHAR(100) NOT NULL
);

CREATE TABLE student_exchange_tools.countries (
  country_code CHAR(2) PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  programme_id SERIAL       NOT NULL
    REFERENCES student_exchange_tools.programmes (programme_id),
  version      INTEGER
);
 
CREATE TABLE student_exchange_tools.addresses (
  address_id  SERIAL PRIMARY KEY,
  street      VARCHAR(80) NOT NULL,
  number      VARCHAR(10)  NOT NULL,
  country     CHAR(2)     NOT NULL
    REFERENCES student_exchange_tools.countries (country_code),
  city        VARCHAR(60) NOT NULL,
  postal_code VARCHAR(10) NOT NULL,
  region      VARCHAR(60),
  version     INTEGER	  NOT NULL
);
 
CREATE TABLE student_exchange_tools.partners (
  partner_id        SERIAL PRIMARY KEY,
  legal_name        VARCHAR(255) NOT NULL,
  business_name     VARCHAR(255) NOT NULL,
  full_name         VARCHAR(255) NOT NULL,
  organisation_type VARCHAR(60)  NOT NULL,
  employee_count    INTEGER      NOT NULL,
  address           INTEGER      NOT NULL
    REFERENCES student_exchange_tools.addresses (address_id),
  email             VARCHAR(255) NOT NULL,
  website           VARCHAR(255) NOT NULL,
  phone_number      VARCHAR(15)  NOT NULL,
  is_official   	BOOLEAN      NOT NULL, -- {T,F}
  is_archived		BOOLEAN		 NOT NULL,
  version     		INTEGER	  	 NOT NULL
);
 
CREATE TABLE student_exchange_tools.partner_options (
  option_code		CHAR(3)
    REFERENCES student_exchange_tools.options (option_code),
  partner_id  		INTEGER
    REFERENCES student_exchange_tools.partners (partner_id),
  departement	  	VARCHAR(100) NOT NULL,
  PRIMARY KEY (option_code, partner_id)
);
 
CREATE TABLE student_exchange_tools.denial_reasons (
  reason_id SERIAL PRIMARY KEY,
  reason    VARCHAR(300) NOT NULL
);
 
CREATE TABLE student_exchange_tools.mobility_choices (
  mobility_choice_id          SERIAL PRIMARY KEY,
  user_id                     SERIAL    NOT NULL
    REFERENCES student_exchange_tools.users (user_id),
  preference_order            INTEGER   NOT NULL,
  mobility_type               CHAR(3)   NOT NULL, -- {SMS,SMP}
  academic_year               INTEGER   NOT NULL,
  term                        INTEGER   NOT NULL, -- {1,2}
  programme                   INTEGER   NOT NULL
    REFERENCES student_exchange_tools.programmes (programme_id),
  country                     CHAR(2)
    REFERENCES student_exchange_tools.countries (country_code),
  submission_date             TIMESTAMP NOT NULL,
  prof_denial_reason          INTEGER
    REFERENCES student_exchange_tools.denial_reasons (reason_id),
  student_cancellation_reason VARCHAR(500),
  partner                     INTEGER
    REFERENCES student_exchange_tools.partners (partner_id),
  version                     INTEGER   NOT NULL
);
 
CREATE TABLE student_exchange_tools.nominated_students (
  user_id            SERIAL PRIMARY KEY
    REFERENCES student_exchange_tools.users (user_id),
  title              VARCHAR(3)   NOT NULL, -- {Mr,M,Mrs,Ms}
  birthdate          TIMESTAMP    NOT NULL,
  nationality        CHAR(2)      NOT NULL
    REFERENCES student_exchange_tools.countries (country_code),
  address            INTEGER      NOT NULL
    REFERENCES student_exchange_tools.addresses (address_id),
  phone_number       VARCHAR(15)  NOT NULL,
  gender             CHAR(1)      NOT NULL, -- {M,F,O}
  passed_years_count INTEGER      NOT NULL,
  iban               VARCHAR(31)  NOT NULL,
  card_holder        VARCHAR(35),
  bank_name          VARCHAR(60)  NOT NULL,
  bic                VARCHAR(12)  NOT NULL,
  version            INTEGER
);
 
CREATE TABLE student_exchange_tools.mobilities (
  mobility_choice_id          INTEGER PRIMARY KEY
    REFERENCES student_exchange_tools.mobility_choices (mobility_choice_id),
  submission_date             TIMESTAMP   NOT NULL,
  state                       VARCHAR(30) NOT NULL,
  state_before_cancellation   VARCHAR(30),
  first_payment_request_date  TIMESTAMP,
  second_payment_request_date TIMESTAMP,
  pro_eco_encoding            BOOLEAN,
  second_software_encoding    BOOLEAN,
  student_cancellation_reason VARCHAR(500),
  prof_denial_reason          INTEGER
    REFERENCES student_exchange_tools.denial_reasons (reason_id),
  professor_in_charge         INTEGER
    REFERENCES student_exchange_tools.users (user_id),
  version                     INTEGER     NOT NULL
);
 
CREATE TABLE student_exchange_tools.documents (
  document_id  SERIAL PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  category     CHAR(1)      NOT NULL, --{D,R}
  programme_id INTEGER      NOT NULL
    REFERENCES student_exchange_tools.programmes (programme_id),
  version      INTEGER      NOT NULL
);
 
CREATE TABLE student_exchange_tools.mobility_documents (
  document_id  INTEGER
    REFERENCES student_exchange_tools.documents (document_id),
  mobility_id  INTEGER
    REFERENCES student_exchange_tools.mobilities (mobility_choice_id),
  is_filled_in BOOLEAN NOT NULL,
  version      INTEGER NOT NULL,
  PRIMARY KEY (document_id, mobility_id)
);
 
-- Inserts IPL Options

INSERT INTO student_exchange_tools.options (option_code, name)
VALUES ('BIN', 'Bachelier en informatique de gestion');
INSERT INTO student_exchange_tools.options (option_code, name)
VALUES ('BBM', 'Bachelier en biologie médicale');
INSERT INTO student_exchange_tools.options (option_code, name)
VALUES ('BCH', 'Bachelier en chimie');
INSERT INTO student_exchange_tools.options (option_code, name)
VALUES ('BDI', 'Bachelier en diététique');
INSERT INTO student_exchange_tools.options (option_code, name)
VALUES ('BIM', 'Bachelier en imagerie médicale');

-- Inserts mobility programmes

INSERT INTO student_exchange_tools.programmes
VALUES (DEFAULT, 'Erasmus+', 'Mobility Tool');
INSERT INTO student_exchange_tools.programmes
VALUES (DEFAULT, 'Erabel', 'Mobi-ERABEL');
INSERT INTO student_exchange_tools.programmes
VALUES (DEFAULT, 'FAME', 'Mobi-FAME');

-- Inserts countries

INSERT INTO student_exchange_tools.countries VALUES
  ('AF', 'Afghanistan', 3, 1),
  ('AL', 'Albanie', 3, 1),
  ('AQ', 'Antarctique', 3, 1),
  ('DZ', 'Algérie', 3, 1),
  ('AS', 'Samoa Américaines', 3, 1),
  ('AD', 'Andorre', 3, 1),
  ('AO', 'Angola', 3, 1),
  ('AG', 'Antigua-et-Barbuda', 3, 1),
  ('AZ', 'Azerbaïdjan', 3, 1),
  ('AR', 'Argentine', 3, 1),
  ('AU', 'Australie', 3, 1),
  ('AT', 'Autriche', 1, 1),
  ('BS', 'Bahamas', 3, 1),
  ('BH', 'Bahreïn', 3, 1),
  ('BD', 'Bangladesh', 3, 1),
  ('AM', 'Arménie', 3, 1),
  ('BB', 'Barbade', 3, 1),
  ('BE', 'Belgique', 2, 1),
  ('BM', 'Bermudes', 3, 1),
  ('BT', 'Bhoutan', 3, 1),
  ('BO', 'Bolivie', 3, 1),
  ('BA', 'Bosnie-Herzégovine', 3, 1),
  ('BW', 'Botswana', 3, 1),
  ('BV', 'Île Bouvet', 3, 1),
  ('BR', 'Brésil', 3, 1),
  ('BZ', 'Belize', 3, 1),
  ('IO', 'Territoire Britannique de l''Océan Indien', 3, 1),
  ('SB', 'Îles Salomon', 3, 1),
  ('VG', 'Îles Vierges Britanniques', 3, 1),
  ('BN', 'Brunéi Darussalam', 3, 1),
  ('BG', 'Bulgarie', 1, 1),
  ('MM', 'Myanmar', 3, 1),
  ('BI', 'Burundi', 3, 1),
  ('BY', 'Bélarus', 3, 1),
  ('KH', 'Cambodge', 3, 1),
  ('CM', 'Cameroun', 3, 1),
  ('CA', 'Canada', 3, 1),
  ('CV', 'Cap-vert', 3, 1),
  ('KY', 'Îles Caïmanes', 3, 1),
  ('CF', 'République Centrafricaine', 3, 1),
  ('LK', 'Sri Lanka', 3, 1),
  ('TD', 'Tchad', 3, 1),
  ('CL', 'Chili', 3, 1),
  ('CN', 'Chine', 3, 1),
  ('TW', 'Taïwan', 3, 1),
  ('CX', 'Île Christmas', 3, 1),
  ('CC', 'Îles Cocos (Keeling)', 3, 1),
  ('CO', 'Colombie', 3, 1),
  ('KM', 'Comores', 3, 1),
  ('YT', 'Mayotte', 3, 1),
  ('CG', 'République du Congo', 3, 1),
  ('CD', 'République Démocratique du Congo', 3, 1),
  ('CK', 'Îles Cook', 3, 1),
  ('CR', 'Costa Rica', 3, 1),
  ('HR', 'Croatie', 1, 1),
  ('CU', 'Cuba', 3, 1),
  ('CY', 'Chypre', 1, 1),
  ('CZ', 'République Tchèque', 3, 1),
  ('BJ', 'Bénin', 3, 1),
  ('DK', 'Danemark', 1, 1),
  ('DM', 'Dominique', 3, 1),
  ('DO', 'République Dominicaine', 3, 1),
  ('EC', 'Équateur', 3, 1),
  ('SV', 'El Salvador', 3, 1),
  ('GQ', 'Guinée Équatoriale', 3, 1),
  ('ET', 'Éthiopie', 3, 1),
  ('ER', 'Érythrée', 3, 1),
  ('EE', 'Estonie', 1, 1),
  ('FO', 'Îles Féroé', 3, 1),
  ('FK', 'Îles (malvinas) Falkland', 3, 1),
  ('GS', 'Géorgie du Sud et les Îles Sandwich du Sud', 3, 1),
  ('FJ', 'Fidji', 3, 1),
  ('FI', 'Finlande', 1, 1),
  ('AX', 'Îles Åland', 3, 1),
  ('FR', 'France', 1, 1),
  ('GF', 'Guyane Française', 3, 1),
  ('PF', 'Polynésie Française', 3, 1),
  ('TF', 'Terres Australes Françaises', 3, 1),
  ('DJ', 'Djibouti', 3, 1),
  ('GA', 'Gabon', 3, 1),
  ('GE', 'Géorgie', 3, 1),
  ('GM', 'Gambie', 3, 1),
  ('PS', 'Territoire Palestinien Occupé', 3, 1),
  ('DE', 'Allemagne', 1, 1),
  ('GH', 'Ghana', 3, 1),
  ('GI', 'Gibraltar', 3, 1),
  ('KI', 'Kiribati', 3, 1),
  ('GR', 'Grèce', 1, 1),
  ('GL', 'Groenland', 3, 1),
  ('GD', 'Grenade', 3, 1),
  ('GP', 'Guadeloupe', 3, 1),
  ('GU', 'Guam', 3, 1),
  ('GT', 'Guatemala', 3, 1),
  ('GN', 'Guinée', 3, 1),
  ('GY', 'Guyana', 3, 1),
  ('HT', 'Haïti', 3, 1),
  ('HM', 'Îles Heard et Mcdonald', 3, 1),
  ('VA', 'Saint-Siège (état de la Cité du Vatican)', 3, 1),
  ('HN', 'Honduras', 3, 1),
  ('HK', 'Hong-Kong', 3, 1),
  ('HU', 'Hongrie', 1, 1),
  ('IS', 'Islande', 1, 1),
  ('IN', 'Inde', 3, 1),
  ('ID', 'Indonésie', 3, 1),
  ('IR', 'République Islamique d''Iran', 3, 1),
  ('IQ', 'Iraq', 3, 1),
  ('IE', 'Irlande', 1, 1),
  ('IL', 'Israël', 3, 1),
  ('IT', 'Italie', 1, 1),
  ('CI', 'Côte d''Ivoire', 3, 1),
  ('JM', 'Jamaïque', 3, 1),
  ('JP', 'Japon', 3, 1),
  ('KZ', 'Kazakhstan', 3, 1),
  ('JO', 'Jordanie', 3, 1),
  ('KE', 'Kenya', 3, 1),
  ('KP', 'République Populaire Démocratique de Corée', 3, 1),
  ('KR', 'République de Corée', 3, 1),
  ('KW', 'Koweït', 3, 1),
  ('KG', 'Kirghizistan', 3, 1),
  ('LA', 'République Démocratique Populaire Lao', 3, 1),
  ('LB', 'Liban', 3, 1),
  ('LS', 'Lesotho', 3, 1),
  ('LV', 'Lettonie', 1, 1),
  ('LR', 'Libéria', 3, 1),
  ('LY', 'Jamahiriya Arabe Libyenne', 3, 1),
  ('LI', 'Liechtenstein', 1, 1),
  ('LT', 'Lituanie', 1, 1),
  ('LU', 'Luxembourg', 1, 1),
  ('MO', 'Macao', 3, 1),
  ('MG', 'Madagascar', 3, 1),
  ('MW', 'Malawi', 3, 1),
  ('MY', 'Malaisie', 3, 1),
  ('MV', 'Maldives', 3, 1),
  ('ML', 'Mali', 3, 1),
  ('MT', 'Malte', 1, 1),
  ('MQ', 'Martinique', 3, 1),
  ('MR', 'Mauritanie', 3, 1),
  ('MU', 'Maurice', 3, 1),
  ('MX', 'Mexique', 3, 1),
  ('MC', 'Monaco', 3, 1),
  ('MN', 'Mongolie', 3, 1),
  ('MD', 'République de Moldova', 3, 1),
  ('MS', 'Montserrat', 3, 1),
  ('MA', 'Maroc', 3, 1),
  ('MZ', 'Mozambique', 3, 1),
  ('OM', 'Oman', 3, 1),
  ('NA', 'Namibie', 3, 1),
  ('NR', 'Nauru', 3, 1),
  ('NP', 'Népal', 3, 1),
  ('NL', 'Pays-Bas', 1, 1),
  ('AN', 'Antilles Néerlandaises', 3, 1),
  ('AW', 'Aruba', 3, 1),
  ('NC', 'Nouvelle-Calédonie', 3, 1),
  ('VU', 'Vanuatu', 3, 1),
  ('NZ', 'Nouvelle-Zélande', 3, 1),
  ('NI', 'Nicaragua', 3, 1),
  ('NE', 'Niger', 3, 1),
  ('NG', 'Nigéria', 3, 1),
  ('NU', 'Niué', 3, 1),
  ('NF', 'Île Norfolk', 3, 1),
  ('NO', 'Norvège', 1, 1),
  ('MP', 'Îles Mariannes du Nord', 3, 1),
  ('UM', 'Îles Mineures Éloignées des États-Unis', 3, 1),
  ('FM', 'États Fédérés de Micronésie', 3, 1),
  ('MH', 'Îles Marshall', 3, 1),
  ('PW', 'Palaos', 3, 1),
  ('PK', 'Pakistan', 3, 1),
  ('PA', 'Panama', 3, 1),
  ('PG', 'Papouasie-Nouvelle-Guinée', 3, 1),
  ('PY', 'Paraguay', 3, 1),
  ('PE', 'Pérou', 3, 1),
  ('PH', 'Philippines', 3, 1),
  ('PN', 'Pitcairn', 3, 1),
  ('PL', 'Pologne', 1, 1),
  ('PT', 'Portugal', 1, 1),
  ('GW', 'Guinée-Bissau', 3, 1),
  ('TL', 'Timor-Leste', 3, 1),
  ('PR', 'Porto Rico', 3, 1),
  ('QA', 'Qatar', 3, 1),
  ('RE', 'Réunion', 3, 1),
  ('RO', 'Roumanie', 1, 1),
  ('RU', 'Fédération de Russie', 3, 1),
  ('RW', 'Rwanda', 3, 1),
  ('SH', 'Sainte-Hélène', 3, 1),
  ('KN', 'Saint-Kitts-et-Nevis', 3, 1),
  ('AI', 'Anguilla', 3, 1),
  ('LC', 'Sainte-Lucie', 3, 1),
  ('PM', 'Saint-Pierre-et-Miquelon', 3, 1),
  ('VC', 'Saint-Vincent-et-les Grenadines', 3, 1),
  ('SM', 'Saint-Marin', 3, 1),
  ('ST', 'Sao Tomé-et-Principe', 3, 1),
  ('SA', 'Arabie Saoudite', 3, 1),
  ('SN', 'Sénégal', 3, 1),
  ('SC', 'Seychelles', 3, 1),
  ('SL', 'Sierra Leone', 3, 1),
  ('SG', 'Singapour', 3, 1),
  ('SK', 'Slovaquie', 1, 1),
  ('VN', 'Viet Nam', 3, 1),
  ('SI', 'Slovénie', 1, 1),
  ('SO', 'Somalie', 3, 1),
  ('ZA', 'Afrique du Sud', 3, 1),
  ('ZW', 'Zimbabwe', 3, 1),
  ('ES', 'Espagne', 1, 1),
  ('EH', 'Sahara Occidental', 3, 1),
  ('SD', 'Soudan', 3, 1),
  ('SR', 'Suriname', 3, 1),
  ('SJ', 'Svalbard etÎle Jan Mayen', 3, 1),
  ('SZ', 'Swaziland', 3, 1),
  ('SE', 'Suède', 1, 1),
  ('CH', 'Suisse', 1, 1),
  ('SY', 'République Arabe Syrienne', 3, 1),
  ('TJ', 'Tadjikistan', 3, 1),
  ('TH', 'Thaïlande', 3, 1),
  ('TG', 'Togo', 3, 1),
  ('TK', 'Tokelau', 3, 1),
  ('TO', 'Tonga', 3, 1),
  ('TT', 'Trinité-et-Tobago', 3, 1),
  ('AE', 'Émirats Arabes Unis', 3, 1),
  ('TN', 'Tunisie', 3, 1),
  ('TR', 'Turquie', 1, 1),
  ('TM', 'Turkménistan', 3, 1),
  ('TC', 'Îles Turks et Caïques', 3, 1),
  ('TV', 'Tuvalu', 3, 1),
  ('UG', 'Ouganda', 3, 1),
  ('UA', 'Ukraine', 3, 1),
  ('MK', 'L''ex-République Yougoslave de Macédoine', 1, 1),
  ('EG', 'Égypte', 3, 1),
  ('GB', 'Royaume-Uni', 1, 1),
  ('IM', 'Île de Man', 3, 1),
  ('TZ', 'République-Unie de Tanzanie', 3, 1),
  ('US', 'États-Unis', 3, 1),
  ('VI', 'Îles Vierges des États-Unis', 3, 1),
  ('BF', 'Burkina Faso', 3, 1),
  ('UY', 'Uruguay', 3, 1),
  ('UZ', 'Ouzbékistan', 3, 1),
  ('VE', 'Venezuela', 3, 1),
  ('WF', 'Wallis et Futuna', 3, 1),
  ('WS', 'Samoa', 3, 1),
  ('YE', 'Yémen', 3, 1),
  ('CS', 'Serbie-et-Monténégro', 3, 1),
  ('ZM', 'Zambie', 3, 1);

-- Inserts departure documents

-- erasmus+
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Contrat de bourse', 'D', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Convention de stage / Convention d''études', 'D', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Charte de l''étudiant', 'D', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Document d''engagement', 'D', 1, 1);
INSERT into student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Preuve du passage des tests linguistiques', 'D', 1, 1);

-- erabel
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Contrat de bourse', 'D', 2, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Convention de stage / Convention d''études', 'D', 2, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version)VALUES ('Charte de l''étudiant', 'D', 2, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Document d''engagement', 'D', 2, 1);

-- fame
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Contrat de bourse', 'D', 3, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Convention de stage / Convention d''études', 'D', 3, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version)VALUES ('Charte de l''étudiant', 'D', 3, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Document d''engagement', 'D', 3, 1);

-- Inserts return documents

-- erasmus+
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Attestation séjour', 'R', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Relevé de notes (SMS) ou certificat de stage (SMP)', 'R', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Rapport final (complété en ligne)', 'R', 1, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Preuve du passage des tests linguistiques après la mobilité', 'R', 1, 1);

-- erabel
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Attestation séjour', 'R', 2, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Relevé de notes (SMS) ou certificat de stage (SMP)', 'R', 2, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Rapport final (complété en ligne)', 'R', 2, 1);

-- fame
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Attestation séjour', 'R', 3, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Relevé de notes (SMS) ou certificat de stage (SMP)', 'R', 3, 1);
INSERT INTO student_exchange_tools.documents (name, category, programme_id, version) VALUES ('Rapport final (complété en ligne)', 'R', 3, 1);

-- Inserts denial reason
INSERT INTO student_exchange_tools.denial_reasons (reason) VALUES ('L''étudiant part en mobilité pour ce semestre.');
