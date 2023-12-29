WITH mapping AS (
    SELECT bp.id as business_partner_id, nextVal('bpdm_sequence') as confidence_id
    FROM business_partners bp
    WHERE bp.stage = 'Output' AND bp.legal_entity_confidence_id IS NULL
),
confidence AS (
     INSERT INTO confidence_criteria (id, created_at, updated_at, uuid, shared_by_owner, checked_by_external_data_source, number_of_business_partners, last_confidence_check_at, next_confidence_check_at, confidence_level)
     SELECT mapping.confidence_id, NOW(), NOW(), gen_random_uuid(), FALSE, FALSE, 1, NOW()::timestamp, NOW()::timestamp, 0
     FROM mapping
)
UPDATE business_partners bp
SET    legal_entity_confidence_id = mapping.confidence_id
FROM   mapping
WHERE  bp.id = mapping.business_partner_id;


WITH mapping AS (
    SELECT bp.id as business_partner_id, nextVal('bpdm_sequence') as confidence_id
    FROM business_partners bp
    WHERE bp.stage = 'Output' AND bp.address_confidence_id IS NULL
),
confidence AS (
     INSERT INTO confidence_criteria (id, created_at, updated_at, uuid, shared_by_owner, checked_by_external_data_source, number_of_business_partners, last_confidence_check_at, next_confidence_check_at, confidence_level)
     SELECT mapping.confidence_id, NOW(), NOW(), gen_random_uuid(), FALSE, FALSE, 1, NOW()::timestamp, NOW()::timestamp, 0
     FROM mapping
)
UPDATE business_partners bp
SET    address_confidence_id = mapping.confidence_id
FROM   mapping
WHERE  bp.id = mapping.business_partner_id;