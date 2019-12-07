-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE undo_log
(
    id            NUMBER(19)    NOT NULL,
    branch_id     NUMBER(19)    NOT NULL,
    xid           VARCHAR2(100) NOT NULL,
    context       VARCHAR2(128) NOT NULL,
    rollback_info BLOB          NOT NULL,
    log_status    NUMBER(10)    NOT NULL,
    log_created   TIMESTAMP(0)  NOT NULL,
    log_modified  TIMESTAMP(0)  NOT NULL,
    ext           VARCHAR2(100) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ux_undo_log UNIQUE (xid, branch_id)
);

COMMENT ON TABLE undo_log IS 'AT transaction mode undo table';

-- Generate ID using sequence and trigger
CREATE SEQUENCE undo_log_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER undo_log_seq_tr
                                                 BEFORE INSERT
                                                            ON undo_log
                                                            FOR EACH ROW
                                                            WHEN (NEW.id IS NULL)
BEGIN
SELECT undo_log_seq.NEXTVAL INTO :NEW.id FROM DUAL;
END;
/