-- the table to store seata xid data
-- 0.7.0+ add context
-- you must to init this sql for you business databese. the seata server not need it.
-- 此脚本必须初始化在你当前的业务数据库中，用于AT 模式XID记录。与server端无关（注：业务数据库）
-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
-- oracle version
-- @author tq02ksu ( tq02ksu@gmail.com )
CREATE TABLE undo_log (
  id number(20) NOT NULL,
  branch_id number(20) NOT NULL,
  xid varchar2(100) NOT NULL,
  context varchar2(128) NOT NULL,
  rollback_info blob NOT NULL,
  log_status number(11) NOT NULL,
  log_created timestamp NOT NULL,
  log_modified timestamp NOT NULL,
  ext varchar2(100) DEFAULT NULL,
  PRIMARY KEY (id)
  )
/
create unique index index_ux_undo_log on undo_log (xid, branch_id)
/
create sequence undo_log_seq
/