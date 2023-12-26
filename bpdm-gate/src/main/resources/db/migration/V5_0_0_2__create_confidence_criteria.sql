CREATE TABLE confidence_criteria(
    id BIGINT NOT NULL,
    created_at TIMESTAMP WITH time zone NOT NULL,
    updated_at TIMESTAMP WITH time zone NOT NULL,
    uuid UUID NOT NULL,
	shared_by_owner                  boolean     NOT NULL,
    checked_by_external_data_source  boolean     NOT NULL,
    number_of_business_partners      INT         NOT NULL,
    last_confidence_check_at         TIMESTAMP   NOT NULL,
    next_confidence_check_at         TIMESTAMP   NOT NULL,
    confidence_level                 INT         NOT NULL,
    CONSTRAINT pk_confidence_criteria PRIMARY KEY (id)
);

ALTER TABLE business_partners
ADD COLUMN legal_entity_confidence_id   BIGINT,
ADD COLUMN site_confidence_id   BIGINT,
ADD COLUMN address_confidence_id   BIGINT;

ALTER TABLE business_partners
ADD CONSTRAINT fk_business_partners_confidence_l FOREIGN KEY (legal_entity_confidence_id) REFERENCES confidence_criteria (id),
ADD CONSTRAINT fk_business_partners_confidence_s FOREIGN KEY (site_confidence_id) REFERENCES confidence_criteria (id),
ADD CONSTRAINT fk_business_partners_confidence_a FOREIGN KEY (address_confidence_id) REFERENCES confidence_criteria (id);





