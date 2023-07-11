-- For LsaType.LEGAL_ENTITY
INSERT INTO sharing_states (external_id, lsa_type, sharing_state_type)
SELECT external_id, 'LEGAL_ENTITY', 'Initial'
FROM legal_entities
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'LEGAL_ENTITY');

-- For LsaType.SITE
INSERT INTO sharing_states (external_id, lsa_type, sharing_state_type)
SELECT external_id, 'SITE', 'Initial'
FROM sites
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'SITE');

-- For LsaType.ADDRESS
INSERT INTO sharing_states (external_id, lsa_type, sharing_state_type)
SELECT external_id, 'ADDRESS', 'Initial'
FROM logistic_addresses
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'ADDRESS');
