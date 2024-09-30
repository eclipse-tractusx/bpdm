ALTER TABLE legal_forms
ALTER COLUMN name TYPE VARCHAR(4000);

ALTER TABLE legal_forms
ALTER COLUMN abbreviation TYPE VARCHAR(1000);

ALTER TABLE legal_forms
ADD COLUMN country_code VARCHAR(10);

ALTER TABLE legal_forms
ADD COLUMN language_code VARCHAR(10);

ALTER TABLE legal_forms
ADD COLUMN region_id BIGINT;

ALTER TABLE legal_forms
ADD COLUMN transliterated_name VARCHAR(4000);

ALTER TABLE legal_forms
ADD COLUMN transliterated_abbreviations VARCHAR(1000);

ALTER TABLE legal_forms
ADD COLUMN is_active BOOLEAN;

