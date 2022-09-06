-- create addresses for legal entities which don't have any yet
INSERT INTO addresses(id, uuid, created_at, updated_at, country, partner_id)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'Undefined', business_partners.id
FROM business_partners
         LEFT JOIN addresses ON business_partners.id = addresses.partner_id
WHERE addresses.id = 0;

-- create addresses for sites which don't have any yet
INSERT INTO addresses(id, uuid, created_at, updated_at, country, site_id)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'Undefined', sites.id
FROM sites
         LEFT JOIN addresses ON sites.id = addresses.site_id
WHERE addresses.id = 0;

-- the first created address of a legal entity will be assigned as a legal address
UPDATE business_partners
SET legal_address_id = subquery.id FROM(
        SELECT id, partner_id, row_number() over(PARTITION BY partner_id  ORDER BY created_at) AS rn
        FROM addresses
        WHERE partner_id IS NOT NULL
) AS subquery
WHERE subquery.partner_id = business_partners.id AND subquery.rn = 1;

-- the first  created address of a site will be assigned as its main address
UPDATE sites
SET main_address_id = subquery.id FROM(
        SELECT id, site_id, row_number() over(PARTITION BY site_id  ORDER BY created_at) AS rn
        FROM addresses
        WHERE site_id IS NOT NULL
    ) AS subquery
WHERE subquery.site_id = sites.id AND subquery.rn = 1;

-- the remaining addresses of a legal entity will be business partner addresses
INSERT INTO address_partners(id, uuid, created_at, updated_at, bpn, legal_entity_id, address_id)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), created_at, updated_at, bpn, partner_id, address_id
FROM (SELECT id as address_id, created_at, updated_at, bpn, partner_id, row_number() over (partition BY partner_id ORDER BY created_at DESC) AS rn
      FROM addresses
      WHERE partner_id IS NOT NULL) AS subquery
WHERE rn > 1;

-- the remaining addresses of a site will be business partner addresses
INSERT INTO address_partners(id, uuid, created_at, updated_at, bpn, site_id, address_id)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), created_at, updated_at, bpn, site_id, address_id
FROM (SELECT id as address_id, created_at, updated_at, bpn, site_id, row_number() over(partition BY site_id  ORDER BY created_at DESC) AS rn
      FROM addresses
      WHERE site_id IS NOT NULL) AS subquery
WHERE rn > 1;
