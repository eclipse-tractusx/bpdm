CREATE OR REPLACE FUNCTION calc_conf_level(
    shared_by_owner BOOLEAN,
    checked_by_external_datasource BOOLEAN,
    sharing_members INT
)
RETURNS INT AS $$
DECLARE
    shared_by_owner_level INT := 0;
    checked_by_external_datasource_level INT := 0;
    sharing_members_level INT := 0;
BEGIN
    -- Assign points if shared by owner
    IF shared_by_owner THEN
        shared_by_owner_level := 5;
    END IF;

    -- Assign points if checked by external datasource
    IF checked_by_external_datasource THEN
        checked_by_external_datasource_level := 3;
    END IF;

    -- Assign points if more than 2 members are sharing
    IF sharing_members > 2 THEN
        sharing_members_level := 1;
    END IF;

    -- Return total confidence level
    RETURN shared_by_owner_level
         + checked_by_external_datasource_level
         + sharing_members_level;
END;
$$ LANGUAGE plpgsql;

UPDATE legal_entities
SET confidence_level = calc_conf_level(shared_by_owner, checked_by_external_data_source, number_of_business_partners),
    updated_at = LOCALTIMESTAMP;

INSERT INTO partner_changelog_entries(id, uuid, created_at, updated_at, changelog_type, bpn, business_partner_type)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'UPDATE', le.bpn, 'LEGAL_ENTITY'
FROM legal_entities le;

UPDATE sites
SET confidence_level = calc_conf_level(shared_by_owner, checked_by_external_data_source, number_of_business_partners),
    updated_at = LOCALTIMESTAMP;

INSERT INTO partner_changelog_entries(id, uuid, created_at, updated_at, changelog_type, bpn, business_partner_type)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'UPDATE', sites.bpn, 'SITE'
FROM sites;

UPDATE logistic_addresses
SET confidence_level = calc_conf_level(shared_by_owner, checked_by_external_data_source, number_of_business_partners),
    updated_at = LOCALTIMESTAMP;

INSERT INTO partner_changelog_entries(id, uuid, created_at, updated_at, changelog_type, bpn, business_partner_type)
SELECT nextval('bpdm_sequence'), gen_random_uuid(), LOCALTIMESTAMP, LOCALTIMESTAMP, 'UPDATE', la.bpn, 'ADDRESS'
FROM logistic_addresses la;
