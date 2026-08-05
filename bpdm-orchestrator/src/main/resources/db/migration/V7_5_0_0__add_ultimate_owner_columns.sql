ALTER TABLE golden_record_tasks
    ADD COLUMN ownership_ultimate BOOLEAN,
    ADD COLUMN ultimate_owner_bpnl VARCHAR(255);
