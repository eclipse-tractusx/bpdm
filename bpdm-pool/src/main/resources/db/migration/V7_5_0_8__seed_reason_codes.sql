INSERT INTO reason_codes (id, uuid, created_at, updated_at, technical_key, description)
VALUES
    (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'LEGAL_ENTITY_COURT_DISTRICT_CHANGE', 'Re-registration of a legal entity in a different court district, generating a new BPNL with new legal registration values'),
    (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'MERGER', 'Merger of two or more legal entities into a single successor entity'),
    (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'SPLIT_SPIN_OFF', 'Split or spin-off of a legal entity into one or more new entities'),
    (nextval('bpdm_sequence'), gen_random_uuid(), now(), now(), 'INSOLVENCY_ABSORPTION', 'Absorption of an insolvent business partner''s remaining assets by another entity')
ON CONFLICT (technical_key) DO NOTHING;
