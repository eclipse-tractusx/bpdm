alter table addresses
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table addresses
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table administrative_areas
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table administrative_areas
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table care_ofs
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table care_ofs
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table configuration_entries
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table configuration_entries
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table identifier_status
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table identifier_status
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table identifier_types
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table identifier_types
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table issuing_bodies
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table issuing_bodies
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table legal_form_categories
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table legal_form_categories
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table legal_forms
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table legal_forms
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table legal_entities
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table legal_entities
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table legal_entities
    alter column currentness type timestamp with time zone using currentness at time zone 'UTC';
alter table bank_accounts
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table bank_accounts
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table classifications
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table classifications
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table identifiers
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table identifiers
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table localities
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table localities
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table names
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table names
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table post_codes
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table post_codes
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table postal_delivery_points
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table postal_delivery_points
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table premises
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table premises
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table relations
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table relations
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table roles
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table roles
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table business_stati
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table business_stati
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table thoroughfares
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table thoroughfares
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table sync_records
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table sync_records
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table sync_records
    alter column started_at type timestamp with time zone using started_at at time zone 'UTC';
alter table sync_records
    alter column finished_at type timestamp with time zone using finished_at at time zone 'UTC';
alter table sync_records
    alter column from_time type timestamp with time zone using from_time at time zone 'UTC';
alter table partner_changelog_entries
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table partner_changelog_entries
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table sites
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table sites
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table address_partners
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table address_partners
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table import_entries
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table import_entries
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';
alter table country_identifier_types
    alter column created_at type timestamp with time zone using created_at at time zone 'UTC';
alter table country_identifier_types
    alter column updated_at type timestamp with time zone using updated_at at time zone 'UTC';