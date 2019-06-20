-- ----------------------------
-- Table structure for UNDO_LOG
-- ----------------------------
DROP TABLE "UNDO_LOG";
CREATE TABLE "UNDO_LOG" (
  "ID" NUMBER(20) VISIBLE NOT NULL ,
  "BRANCH_ID" NUMBER(20) VISIBLE ,
  "XID" VARCHAR2(100 BYTE) VISIBLE ,
  "CONTEXT" VARCHAR2(128 BYTE) VISIBLE ,
  "ROLLBACK_INFO" BLOB VISIBLE ,
  "LOG_STATUS" NUMBER(11) VISIBLE ,
  "LOG_CREATED" TIMESTAMP(6) VISIBLE ,
  "LOG_MODIFIED" TIMESTAMP(6) VISIBLE ,
  "EXT" VARCHAR2(100 BYTE) VISIBLE 
)
-- ----------------------------
-- Primary Key structure for table UNDO_LOG
-- ----------------------------
ALTER TABLE "CSM_TEST"."UNDO_LOG" ADD CONSTRAINT "SYS_C0020748" PRIMARY KEY ("ID");

-- ----------------------------
-- Uniques structure for table UNDO_LOG
-- ----------------------------
ALTER TABLE "CSM_TEST"."UNDO_LOG" ADD CONSTRAINT "SYS_C0020749" UNIQUE ("XID", "BRANCH_ID") NOT DEFERRABLE INITIALLY IMMEDIATE NORELY VALIDATE;

-- ----------------------------
-- Checks structure for table UNDO_LOG
-- ----------------------------
ALTER TABLE "CSM_TEST"."UNDO_LOG" ADD CONSTRAINT "SYS_C0020747" CHECK ("ID" IS NOT NULL) NOT DEFERRABLE INITIALLY IMMEDIATE NORELY VALIDATE;
