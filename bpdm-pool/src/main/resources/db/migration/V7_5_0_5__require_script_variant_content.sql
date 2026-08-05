-- A script variant is a business partner written out completely in another script. It therefore has to carry the
-- same mandatory content as the invariant data it mirrors, and that is now part of the API contract rather than
-- something validated after the fact. Rows that predate the contract are removed before the columns are tightened.

-- Variants that carry no text at all in the field the contract requires.
DELETE FROM legal_entity_script_variants WHERE legal_name IS NULL OR trim(legal_name) = '';
DELETE FROM site_script_variants WHERE name IS NULL OR trim(name) = '';
DELETE FROM address_script_variants WHERE phy_city IS NULL OR trim(phy_city) = '';

-- Names in a script the owning address does not cover. A legal entity or site script variant carries both the name
-- and the address of its script code, so one without the other cannot be represented any more.
DELETE FROM legal_entity_script_variants variant
WHERE NOT EXISTS (
    SELECT 1
    FROM legal_entities entity
             JOIN address_script_variants address_variant
                  ON address_variant.logistic_address_id = entity.legal_address_id
    WHERE entity.id = variant.legal_entity_id
      AND address_variant.script_code_id = variant.script_code_id
);

DELETE FROM site_script_variants variant
WHERE NOT EXISTS (
    SELECT 1
    FROM sites site
             JOIN address_script_variants address_variant
                  ON address_variant.logistic_address_id = site.main_address_id
    WHERE site.id = variant.site_id
      AND address_variant.script_code_id = variant.script_code_id
);

ALTER TABLE legal_entity_script_variants ALTER COLUMN legal_name SET NOT NULL;
ALTER TABLE site_script_variants ALTER COLUMN name SET NOT NULL;
ALTER TABLE address_script_variants ALTER COLUMN phy_city SET NOT NULL;
