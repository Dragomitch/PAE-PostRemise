-- Adds the current ISO 3166 countries missing from databases initialised with an older
-- init.sql (their flags already ship with the web UI). Idempotent: rows that exist are left alone.
-- Run it once against an existing database, e.g.
--   psql -U <user> -d <db> -f SQLRessources/add-missing-countries.sql
INSERT INTO student_exchange_tools.countries VALUES
  ('RS', 'Serbie', 1, 1),
  ('ME', 'Monténégro', 3, 1),
  ('SS', 'Soudan du Sud', 3, 1),
  ('CW', 'Curaçao', 3, 1),
  ('SX', 'Saint-Martin (partie néerlandaise)', 3, 1),
  ('BQ', 'Bonaire, Saint-Eustache et Saba', 3, 1),
  ('BL', 'Saint-Barthélemy', 3, 1),
  ('GG', 'Guernesey', 3, 1),
  ('JE', 'Jersey', 3, 1)
ON CONFLICT (country_code) DO NOTHING;
