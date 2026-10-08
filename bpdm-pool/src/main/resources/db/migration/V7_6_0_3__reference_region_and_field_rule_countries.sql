DO $$
DECLARE
    unresolved_codes text;
BEGIN
    SELECT string_agg(DISTINCT code, ', ' ORDER BY code)
    INTO unresolved_codes
    FROM (
        SELECT r.country_code AS code
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
    ADD CONSTRAINT fk_regions_country_code FOREIGN KEY (country_code) REFERENCES countries (country_code);

ALTER TABLE field_quality_rule
    ADD CONSTRAINT fk_field_quality_rule_country_code FOREIGN KEY (country_code) REFERENCES countries (country_code);
