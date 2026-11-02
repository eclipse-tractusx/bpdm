DO $$
DECLARE
    unresolved_codes text;
BEGIN
    SELECT string_agg(DISTINCT code, ', ' ORDER BY code)
    INTO unresolved_codes
    FROM (
        SELECT r.country_code AS code
        FROM legal_forms r
        LEFT JOIN countries c ON c.country_code = r.country_code
        WHERE r.country_code IS NOT NULL AND c.id IS NULL
        UNION ALL
        SELECT r.country_code AS code
        FROM identifier_type_details r
        LEFT JOIN countries c ON c.country_code = r.country_code
        WHERE r.country_code IS NOT NULL AND c.id IS NULL
    ) unknown;

    IF unresolved_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Legal form or identifier type countries not found in countries catalogue: %', unresolved_codes;
    END IF;
END
$$;

ALTER TABLE legal_forms ADD COLUMN country_id BIGINT;
ALTER TABLE identifier_type_details ADD COLUMN country_id BIGINT;

UPDATE legal_forms r
SET country_id = c.id
FROM countries c
WHERE c.country_code = r.country_code;

UPDATE identifier_type_details r
SET country_id = c.id
FROM countries c
WHERE c.country_code = r.country_code;

ALTER TABLE legal_forms
    ADD CONSTRAINT fk_legal_forms_country FOREIGN KEY (country_id) REFERENCES countries (id),
    DROP COLUMN country_code;

ALTER TABLE identifier_type_details
    DROP CONSTRAINT uc_identifier_type_details_country_code_identifier_type_id,
    ADD CONSTRAINT fk_identifier_type_details_country FOREIGN KEY (country_id) REFERENCES countries (id),
    DROP COLUMN country_code,
    ADD CONSTRAINT uc_identifier_type_details_country_identifier_type UNIQUE (country_id, identifier_type_id);

CREATE INDEX idx_legal_forms_country_id ON legal_forms (country_id);
