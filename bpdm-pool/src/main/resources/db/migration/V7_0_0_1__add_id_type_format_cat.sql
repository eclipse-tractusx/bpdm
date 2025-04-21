ALTER TABLE identifier_types
ADD COLUMN format VARCHAR(255);

CREATE TABLE identifier_type_categories
(
    identifier_type_id BIGINT NOT NULL,
    category VARCHAR(255),
    CONSTRAINT fk_identifier_categories_types FOREIGN KEY (identifier_type_id) REFERENCES identifier_types (id),
    CONSTRAINT uc_identifier_categories UNIQUE (identifier_type_id, category)
);
