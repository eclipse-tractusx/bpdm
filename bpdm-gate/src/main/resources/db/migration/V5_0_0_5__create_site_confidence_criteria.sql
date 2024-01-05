WITH mapping AS (
    SELECT bp.id as business_partner_id, nextVal('bpdm_sequence') as confidence_id
    FROM business_partners bp INNER JOIN postal_addresses pa ON bp.postal_address_id = pa.id
    WHERE bp.stage = 'Output' AND bp.site_confidence_id IS NULL AND (pa.address_type = 'SiteMainAddress' OR pa.address_type = 'LegalAndSiteMainAddress')
),
confidence AS (
     INSERT INTO confidence_criteria (id, created_at, updated_at, uuid, shared_by_owner, checked_by_external_data_source, number_of_business_partners, last_confidence_check_at, next_confidence_check_at, confidence_level)
     SELECT mapping.confidence_id, NOW(), NOW(), gen_random_uuid(), FALSE, FALSE, 1, NOW()::timestamp, NOW()::timestamp, 0
     FROM mapping
)
UPDATE business_partners bp
SET    site_confidence_id = mapping.confidence_id
FROM   mapping
WHERE  bp.id = mapping.business_partner_id;