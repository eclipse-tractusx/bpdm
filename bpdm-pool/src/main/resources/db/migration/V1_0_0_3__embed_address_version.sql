ALTER TABLE addresses
    DROP version_id CASCADE;

/* Character set and languages were wrongly stored as Integer before.
   Create new columns as VARCHAR but don't migrate old data as CDQ has backup. */
ALTER TABLE addresses
    ADD character_set VARCHAR(255) NOT NULL DEFAULT 'UNDEFINED';

ALTER TABLE addresses
    ADD language VARCHAR(255) NOT NULL DEFAULT 'undefined';


DROP TABLE address_versions CASCADE;

