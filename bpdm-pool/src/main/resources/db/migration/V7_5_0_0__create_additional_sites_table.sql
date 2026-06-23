CREATE TABLE address_additional_sites
(
    address_id                  BIGINT                      NOT NULL,
    site_id                     BIGINT                      NOT NULL,
    CONSTRAINT uc_address_additional_sites UNIQUE (address_id, site_id),
    CONSTRAINT fk_address_additional_sites_address FOREIGN KEY (address_id) REFERENCES logistic_addresses(id),
    CONSTRAINT fk_address_additional_sites_site FOREIGN KEY (site_id) REFERENCES sites(id)
);