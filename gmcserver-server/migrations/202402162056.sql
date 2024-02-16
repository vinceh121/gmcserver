-- DROP SCHEMA public;

CREATE SCHEMA public AUTHORIZATION postgres;

COMMENT ON SCHEMA public IS 'standard public schema';
-- public.users definition

-- Drop table

-- DROP TABLE public.users;

CREATE TABLE public.users (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	username varchar NOT NULL,
	"password" varchar NULL,
	email varchar NOT NULL,
	"deviceLimit" int4 NOT NULL,
	"gmcId" int8 NOT NULL,
	"admin" bool NOT NULL,
	mfa bool NOT NULL,
	"alertEmails" bool NOT NULL,
	"mfaKey" jsonb NULL,
	CONSTRAINT users_pk PRIMARY KEY (id)
);
CREATE INDEX users_email_idx ON public.users USING btree (email);
CREATE UNIQUE INDEX users_gmcid_idx ON public.users USING btree ("gmcId");
CREATE INDEX users_username_idx ON public.users USING btree (username);


-- public.calendar definition

-- Drop table

-- DROP TABLE public.calendar;

CREATE TABLE public.calendar (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	"deviceId" uuid NOT NULL,
	"createdAt" timestamp NOT NULL,
	recs jsonb NOT NULL,
	"inProgress" bool NOT NULL
);
COMMENT ON TABLE public.calendar IS 'Single day averages of devices';


-- public.devices definition

-- Drop table

-- DROP TABLE public.devices;

CREATE TABLE public.devices (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	model varchar NULL,
	"name" varchar NULL,
	importedfrom varchar NULL,
	"location" point NULL,
	"owner" uuid NOT NULL,
	lastrecordid uuid NULL,
	gmcid int8 NULL,
	disabled bool NOT NULL,
	lastemailalert timestamp NOT NULL,
	stddevalertlimit float8 NULL,
	proxiessettings jsonb NULL,
	CONSTRAINT devices_pk PRIMARY KEY (id)
);
CREATE UNIQUE INDEX devices_gmcid_idx ON public.devices USING btree (gmcid);


-- public.records definition

-- Drop table

-- DROP TABLE public.records;

CREATE TABLE public.records (
	id uuid NOT NULL DEFAULT gen_random_uuid(),
	"deviceId" uuid NOT NULL,
	"date" timestamp NOT NULL,
	ip inet NULL,
	"type" varchar NULL,
	"location" point NULL,
	cpm float8 NULL,
	acpm float8 NULL,
	usv float8 NULL,
	co2 float8 NULL,
	hcho float8 NULL,
	tmp float8 NULL,
	ap float8 NULL,
	hmdt float8 NULL,
	accy float8 NULL,
	CONSTRAINT records_pk PRIMARY KEY (id)
);
CREATE INDEX records_deviceid_date_idx ON public.records USING btree ("deviceId", date);


-- public.calendar foreign keys

ALTER TABLE public.calendar ADD CONSTRAINT calendar_devices_fk FOREIGN KEY ("deviceId") REFERENCES public.devices(id) ON DELETE CASCADE;


-- public.devices foreign keys

ALTER TABLE public.devices ADD CONSTRAINT devices_lastrecordid_fk FOREIGN KEY (lastrecordid) REFERENCES public.records(id) ON DELETE CASCADE;
ALTER TABLE public.devices ADD CONSTRAINT devices_owner_fk FOREIGN KEY ("owner") REFERENCES public.users(id) ON DELETE CASCADE;


-- public.records foreign keys

ALTER TABLE public.records ADD CONSTRAINT records_fk FOREIGN KEY ("deviceId") REFERENCES public.devices(id) ON DELETE CASCADE;
