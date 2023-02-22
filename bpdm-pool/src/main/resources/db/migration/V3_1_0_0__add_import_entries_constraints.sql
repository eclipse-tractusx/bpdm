ALTER TABLE import_entries ADD PRIMARY KEY (id);
ALTER TABLE import_entries ADD CONSTRAINT uc_import_entries_uuid UNIQUE (uuid);
ALTER TABLE import_entries ADD CONSTRAINT uc_import_entries_bpn UNIQUE (import_id);
