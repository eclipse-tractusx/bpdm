-- A site's name must be unique within its owning legal entity.
ALTER TABLE sites
    ADD CONSTRAINT uc_sites_legal_entity_name UNIQUE (legal_entity_id, name);
