-- the table to store seata xid data
-- 0.7.0+ add context
-- you must to init this sql for you business databese. the seata server not need it.
-- 此脚本必须初始化在你当前的业务数据库中，用于AT 模式XID记录。与server端无关（注：业务数据库）
-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
DROP TABLE IF	EXISTS "undo_log";
CREATE TABLE "undo_log" (
"id" INT8 NOT NULL,
"branch_id" INT8 NOT NULL,
"xid" VARCHAR (100) NOT NULL,
"context" VARCHAR (128) NOT NULL,
"rollback_info" BYTEA NOT NULL,
"log_status" INT4 NOT NULL,
"log_created" TIMESTAMP,
"log_modified" TIMESTAMP,
"ext" VARCHAR (100) DEFAULT NULL,
PRIMARY KEY ("id"),
UNIQUE ("branch_id", "xid")
);