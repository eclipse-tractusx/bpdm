UPDATE  logistic_addresses
    SET legal_entity_id = sites.legal_entity_id
FROM sites
WHERE sites.id = logistic_addresses.site_id;