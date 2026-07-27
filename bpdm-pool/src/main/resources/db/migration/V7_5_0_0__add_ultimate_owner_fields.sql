ALTER TABLE legal_entities
    ADD COLUMN ownership_ultimate BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE legal_entities
    ADD COLUMN ultimate_owner_bpnl VARCHAR(255);