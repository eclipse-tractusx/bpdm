-- Unify an address's site membership into a single many-to-many relationship. Replaces the former single
-- logistic_addresses.site_id FK; the API's bpnSite is now derived (oldest member) and the rest become additionalSites.
CREATE TABLE address_sites
(
    address_id BIGINT NOT NULL,
    site_id    BIGINT NOT NULL,
    CONSTRAINT uc_address_sites UNIQUE (address_id, site_id),
    CONSTRAINT fk_address_sites_address FOREIGN KEY (address_id) REFERENCES logistic_addresses (id),
    CONSTRAINT fk_address_sites_site FOREIGN KEY (site_id) REFERENCES sites (id)
);

-- Carry existing single-site memberships over.
INSERT INTO address_sites (address_id, site_id)
SELECT id, site_id
FROM logistic_addresses
WHERE site_id IS NOT NULL;

ALTER TABLE logistic_addresses
    DROP COLUMN site_id;
