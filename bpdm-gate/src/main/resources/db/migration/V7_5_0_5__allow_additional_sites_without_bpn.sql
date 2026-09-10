-- An additional site stated on the input side may name a site that has no BPNS yet, so the BPN becomes optional.
ALTER TABLE business_partners_additional_sites
    ALTER COLUMN bpn DROP NOT NULL;
