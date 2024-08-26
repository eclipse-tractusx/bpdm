ALTER TABLE identifier_types
ADD COLUMN abbreviation VARCHAR(255);

ALTER TABLE identifier_types
ADD COLUMN transliterated_name VARCHAR(255);

ALTER TABLE identifier_types
ADD COLUMN transliterated_abbreviation VARCHAR(255);