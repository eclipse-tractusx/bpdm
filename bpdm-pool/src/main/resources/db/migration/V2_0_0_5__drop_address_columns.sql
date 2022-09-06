ALTER TABLE addresses
    DROP
        COLUMN bpn;

ALTER TABLE addresses
    DROP
        COLUMN partner_id;

ALTER TABLE addresses
    DROP
        COLUMN site_id;

ALTER TABLE business_partners
    ALTER COLUMN legal_address_id SET NOT NULL;

ALTER TABLE sites
    ALTER COLUMN main_address_id SET NOT NULL;