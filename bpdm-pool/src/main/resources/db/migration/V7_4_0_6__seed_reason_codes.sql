INSERT INTO reason_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'HEADQUARTER_RELOCATION', 'Complete relocation of a legal entity headquarter to a new physical location');
