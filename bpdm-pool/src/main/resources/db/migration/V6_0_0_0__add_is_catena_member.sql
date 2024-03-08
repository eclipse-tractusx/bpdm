ALTER TABLE legal_entities
    ADD is_catena_member boolean;

UPDATE  legal_entities
    SET is_catena_member = case when shared_by_owner is NULL then false else true end
WHERE
    is_catena_member is NULL;


ALTER TABLE legal_entities ALTER COLUMN is_catena_member SET NOT NULL;