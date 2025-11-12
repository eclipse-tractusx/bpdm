ALTER TABLE legal_entities
    ADD COLUMN legal_name_normalized VARCHAR(255);

ALTER TABLE logistic_addresses
    ADD COLUMN phy_street_name_normalized VARCHAR(255);

ALTER TABLE logistic_addresses
    ADD COLUMN phy_country_normalized VARCHAR(255) DEFAULT '';

UPDATE legal_entities
SET legal_name_normalized = replace(
        replace(
            replace(
                replace(trim(regexp_replace(lower(name_value), '\\s+', ' ', 'g')), 'ä', 'ae'),
                'ö', 'oe'
            ),
            'ü', 'ue'
        ),
        'ß', 'ss'
    );

UPDATE logistic_addresses
SET phy_street_name_normalized = CASE
        WHEN phy_street_name IS NULL THEN NULL
        ELSE replace(
            replace(
                replace(
                    replace(trim(regexp_replace(lower(phy_street_name), '\\s+', ' ', 'g')), 'ä', 'ae'),
                    'ö', 'oe'
                ),
                'ü', 'ue'
            ),
            'ß', 'ss'
        )
    END;

UPDATE logistic_addresses
SET phy_country_normalized = replace(
        replace(
            replace(
                replace(trim(regexp_replace(lower(phy_country), '\\s+', ' ', 'g')), 'ä', 'ae'),
                'ö', 'oe'
            ),
            'ü', 'ue'
        ),
        'ß', 'ss'
    );

ALTER TABLE legal_entities
    ALTER COLUMN legal_name_normalized SET NOT NULL;

ALTER TABLE logistic_addresses
    ALTER COLUMN phy_country_normalized SET NOT NULL,
    ALTER COLUMN phy_country_normalized DROP DEFAULT;

CREATE INDEX idx_legal_entities_legal_name_normalized ON legal_entities (legal_name_normalized);
CREATE INDEX idx_logistic_addresses_phy_street_name_normalized ON logistic_addresses (phy_street_name_normalized);
CREATE INDEX idx_logistic_addresses_phy_country_normalized ON logistic_addresses (phy_country_normalized);