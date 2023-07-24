CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- For LsaType.LEGAL_ENTITY
INSERT INTO sharing_states (id, external_id, lsa_type, sharing_state_type, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), external_id, 'LEGAL_ENTITY', 'Initial', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid_generate_v4()
FROM (
    SELECT DISTINCT external_id
    FROM legal_entities
) AS distinct_legal_entities
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'LEGAL_ENTITY');

-- For LsaType.SITE
INSERT INTO sharing_states (id, external_id, lsa_type, sharing_state_type, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), external_id, 'SITE', 'Initial', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid_generate_v4()
FROM (
    SELECT DISTINCT external_id
    FROM sites
) AS distinct_sites
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'SITE');

-- For LsaType.ADDRESS
INSERT INTO sharing_states (id, external_id, lsa_type, sharing_state_type, created_at, updated_at, uuid)
SELECT nextval('bpdm_sequence'), external_id, 'ADDRESS', 'Initial', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, uuid_generate_v4()
FROM (
    SELECT DISTINCT external_id
    FROM logistic_addresses
) AS distinct_logistic_addresses
WHERE external_id NOT IN (SELECT external_id FROM sharing_states WHERE lsa_type = 'ADDRESS');
