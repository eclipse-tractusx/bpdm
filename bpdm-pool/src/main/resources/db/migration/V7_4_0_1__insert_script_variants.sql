INSERT INTO script_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'CHINESE_SIMPLIFIED', 'Chinese Simplified');

INSERT INTO script_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'CHINESE_TRADITIONAL', 'Chinese Traditional');

INSERT INTO script_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'KANJI', 'Kanji');

INSERT INTO script_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'HANGUL', 'Hangul');