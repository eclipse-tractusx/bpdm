DO $$
DECLARE
    unresolved_codes text;
BEGIN
    SELECT string_agg(DISTINCT code, ', ' ORDER BY code)
    INTO unresolved_codes
    FROM (
        SELECT COALESCE(a.phy_country, '<NULL>') AS code
        FROM logistic_addresses a
        LEFT JOIN countries c ON c.country_code = a.phy_country
        WHERE c.id IS NULL
        UNION ALL
        SELECT COALESCE(a.alt_country, '<NULL>') AS code
        FROM logistic_addresses a
        LEFT JOIN countries c ON c.country_code = a.alt_country
        WHERE (a.alt_country IS NOT NULL OR num_nonnulls(
                   a.alt_latitude, a.alt_longitude, a.alt_altitude, a.alt_admin_area_l1_region,
                   a.alt_postcode, a.alt_city, a.alt_delivery_service_type,
                   a.alt_delivery_service_number, a.alt_delivery_service_qualifier
               ) > 0)
          AND c.id IS NULL
    ) unknown;

    IF unresolved_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Address countries not found in countries catalogue: %', unresolved_codes;
    END IF;
END
$$;

ALTER TABLE logistic_addresses
    ADD COLUMN phy_country_id BIGINT,
    ADD COLUMN alt_country_id BIGINT;

UPDATE logistic_addresses a
SET phy_country_id = (SELECT c.id FROM countries c WHERE c.country_code = a.phy_country),
    alt_country_id = (SELECT c.id FROM countries c WHERE c.country_code = a.alt_country);

ALTER TABLE logistic_addresses
    ALTER COLUMN phy_country_id SET NOT NULL,
    ADD CONSTRAINT fk_logistic_addresses_phy_country FOREIGN KEY (phy_country_id) REFERENCES countries (id),
    ADD CONSTRAINT fk_logistic_addresses_alt_country FOREIGN KEY (alt_country_id) REFERENCES countries (id),
    ADD CONSTRAINT ck_logistic_addresses_alt_country_required
        CHECK (num_nonnulls(
                   alt_latitude, alt_longitude, alt_altitude, alt_admin_area_l1_region,
                   alt_postcode, alt_city, alt_delivery_service_type,
                   alt_delivery_service_number, alt_delivery_service_qualifier
               ) = 0 OR alt_country_id IS NOT NULL),
    DROP COLUMN phy_country,
    DROP COLUMN alt_country;

CREATE INDEX idx_logistic_addresses_phy_country_id ON logistic_addresses (phy_country_id);
CREATE INDEX idx_logistic_addresses_alt_country_id ON logistic_addresses (alt_country_id);
