CREATE TABLE business_partners_additional_sites
(
    business_partner_id BIGINT       NOT NULL,
    bpn                 VARCHAR(255) NOT NULL,
    site_name           VARCHAR(255),
    CONSTRAINT fk_additional_sites_business_partners FOREIGN KEY (business_partner_id) REFERENCES business_partners (id)
);

CREATE INDEX index_additional_sites_bp_id ON business_partners_additional_sites (business_partner_id);
