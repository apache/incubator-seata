-- undo_log
CREATE TABLE "public"."undo_log"
(
    "id"            int8                                        NOT NULL,
    "branch_id"     int8                                        NOT NULL,
    "xid"           varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
    "context"       varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "rollback_info" bytea                                       NOT NULL,
    "log_status"    int2                                        NOT NULL,
    "log_created"   timestamp(6)                                NOT NULL,
    "log_modified"  timestamp(6)                                NOT NULL,
    "ext"           varchar(100) COLLATE "pg_catalog"."default",
    CONSTRAINT "undo_log_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "public"."undo_log"
    OWNER TO "postgres";

CREATE UNIQUE INDEX "ux_undo_log" ON "public"."undo_log" USING btree (
    "branch_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "xid" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

CREATE SEQUENCE undo_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE CACHE 1;
alter table "public"."undo_log"
    alter column id set default nextval('undo_log_id_seq');

-- currency type
CREATE TABLE "public"."CURRENCY_TEST"
(
    "MONEY_TEST" money NOT NULL,
    "ID"         int4  NOT NULL,
    CONSTRAINT "CURRENCY_TEST_pkey" PRIMARY KEY ("ID")
)
;
ALTER TABLE "public"."CURRENCY_TEST"
    OWNER TO "postgres";