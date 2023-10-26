CREATE TABLE sync_records (
	id int8 NOT NULL,
	uuid uuid NOT NULL,
	created_at timestamptz NOT NULL,
	updated_at timestamptz NOT NULL,
	"type" varchar(255) NOT NULL,
	status varchar(255) NOT NULL,
	progress float8 NOT NULL,
	count int4 NOT NULL,
	status_details varchar(255) NULL,
	save_state varchar(255) NULL,
	started_at timestamptz NULL,
	finished_at timestamptz NULL,
	from_time timestamptz NOT NULL DEFAULT '1970-01-01 08:00:00'::timestamp without time zone,
	CONSTRAINT pk_sync_records PRIMARY KEY (id),
	CONSTRAINT uc_sync_records_type UNIQUE (type),
	CONSTRAINT uc_sync_records_uuid UNIQUE (uuid)
);