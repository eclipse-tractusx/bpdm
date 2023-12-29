ALTER TABLE legal_entities
ADD COLUMN shared_by_owner                  boolean     NOT NULL,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL,
ADD COLUMN number_of_business_partners      INT         NOT NULL,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN confidence_level                 INT         NOT NULL;

ALTER TABLE sites
ADD COLUMN shared_by_owner                  boolean     NOT NULL,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL,
ADD COLUMN number_of_business_partners      INT         NOT NULL,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN confidence_level                 INT         NOT NULL;

ALTER TABLE logistic_addresses
ADD COLUMN shared_by_owner                  boolean     NOT NULL,
ADD COLUMN checked_by_external_data_source  boolean     NOT NULL,
ADD COLUMN number_of_business_partners      INT         NOT NULL,
ADD COLUMN last_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN next_confidence_check_at         TIMESTAMP   NOT NULL,
ADD COLUMN confidence_level                 INT     NOT NULL;

