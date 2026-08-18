-- A script variant only carries data that is written differently in another script. Postal codes, the tax
-- jurisdiction code, house numbers and the delivery service qualifier and number are assigned identifiers,
-- not transliterable text, so they are no longer part of a script variant.
ALTER TABLE business_partner_script_variants
    DROP COLUMN phy_postcode,
    DROP COLUMN phy_company_postcode,
    DROP COLUMN phy_tax_jurisdiction,
    DROP COLUMN phy_street_number,
    DROP COLUMN phy_street_number_supplement,
    DROP COLUMN phy_street_milestone,
    DROP COLUMN alt_postcode,
    DROP COLUMN alt_delivery_service_qualifier,
    DROP COLUMN alt_delivery_service_number;
