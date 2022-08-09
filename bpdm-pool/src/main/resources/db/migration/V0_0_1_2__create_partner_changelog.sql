CREATE TABLE partner_changelog_entries
(
    id             BIGINT       NOT NULL,
    uuid           UUID         NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    changelog_type VARCHAR(255) NOT NULL,
    bpn            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_partner_changelog_entries PRIMARY KEY (id)
);

ALTER TABLE partner_changelog_entries
    ADD CONSTRAINT uc_partner_changelog_entries_uuid UNIQUE (uuid);