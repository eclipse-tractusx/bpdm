DO $$
DECLARE
    unresolved_codes text;
BEGIN
    SELECT string_agg(DISTINCT code, ', ' ORDER BY code)
    INTO unresolved_codes
    FROM (
        SELECT COALESCE(r.country_code, '<NULL>') AS code
        FROM regions r
        LEFT JOIN countries c ON c.country_code = r.country_code
        WHERE c.id IS NULL
        UNION ALL
        SELECT r.country_code AS code
        FROM field_quality_rule r
        LEFT JOIN countries c ON c.country_code = r.country_code
        WHERE r.country_code IS NOT NULL AND c.id IS NULL
    ) unknown;

    IF unresolved_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Region or field quality rule countries not found in countries catalogue: %', unresolved_codes;
    END IF;
END
$$;

ALTER TABLE regions
    ADD COLUMN country_id BIGINT;

ALTER TABLE field_quality_rule
    ADD COLUMN country_id BIGINT;

UPDATE regions r
SET country_id = c.id
FROM countries c
WHERE c.country_code = r.country_code;

UPDATE field_quality_rule r
SET country_id = c.id
FROM countries c
WHERE c.country_code = r.country_code;

ALTER TABLE regions
    ALTER COLUMN country_id SET NOT NULL,
    ADD CONSTRAINT fk_regions_country FOREIGN KEY (country_id) REFERENCES countries (id),
    DROP COLUMN country_code;

ALTER TABLE field_quality_rule
    DROP CONSTRAINT uc_field_quality_rule_country_schema_field_id,
    ADD CONSTRAINT fk_field_quality_rule_country FOREIGN KEY (country_id) REFERENCES countries (id),
    DROP COLUMN country_code,
    ADD CONSTRAINT uc_field_quality_rule_country_schema_field_id UNIQUE (country_id, field_path, schema_name);

CREATE INDEX idx_regions_country_id ON regions (country_id);
