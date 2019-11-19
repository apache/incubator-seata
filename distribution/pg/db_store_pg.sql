-- the table to store GlobalSession data
DROP TABLE IF	EXISTS "global_table";
CREATE TABLE "global_table" (
"xid" VARCHAR (128) NOT NULL,
"transaction_id" INT8,
"status" INT4 NOT NULL,
"application_id" VARCHAR (32),
"transaction_service_group" VARCHAR (32),
"transaction_name" VARCHAR (64),
"timeout" INT4,
"begin_time" INT8,
"application_data" text,
"gmt_create" TIMESTAMP,
"gmt_modified" TIMESTAMP,
PRIMARY KEY ("xid")
);
CREATE INDEX ON "global_table" ("gmt_modified", "status");
CREATE INDEX ON "global_table" ("transaction_id");

-- the table to store BranchSession data
DROP TABLE IF EXISTS "branch_table";
CREATE TABLE "branch_table" (
"branch_id" INT8 NOT NULL,
"xid" VARCHAR (128) NOT NULL,
"transaction_id" INT8,
"resource_group_id" VARCHAR (32),
"resource_id" VARCHAR (256),
"lock_key" VARCHAR (128),
"branch_type" VARCHAR (8),
"status" INT4,
"client_id" VARCHAR (64),
"application_data" text,
"gmt_create" TIMESTAMP,
"gmt_modified" TIMESTAMP,
PRIMARY KEY ("branch_id")
);
CREATE INDEX ON "branch_table" ("xid");

-- the table to store lock data
DROP TABLE IF EXISTS "lock_table";
CREATE TABLE "lock_table" (
"row_key" VARCHAR (128) NOT NULL,
"xid" VARCHAR (96),
"transaction_id" INT8,
"branch_id" INT8,
"resource_id" VARCHAR (256),
"table_name" VARCHAR (32),
"pk" VARCHAR (32),
"gmt_create" TIMESTAMP,
"gmt_modified" TIMESTAMP,
PRIMARY KEY ("row_key")
);