ALTER TABLE relations DROP COLUMN valid_from;
ALTER TABLE relations DROP COLUMN valid_to;

ALTER TABLE relations ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE SEQUENCE relations_id_seq OWNED BY relations.id;
ALTER TABLE relations ALTER COLUMN id SET DEFAULT nextval('relations_id_seq');
