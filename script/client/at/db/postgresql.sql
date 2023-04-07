-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS public.undo_log
(
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info BYTEA        NOT NULL,
    log_status    INT          NOT NULL,
    log_created   TIMESTAMP(0) NOT NULL,
    log_modified  TIMESTAMP(0) NOT NULL,
    PRIMARY KEY (branch_id)
);
CREATE INDEX ix_log_created ON undo_log(log_created);

COMMENT ON TABLE public.undo_log IS 'AT transaction mode undo table';
COMMENT ON COLUMN public.undo_log.branch_id IS 'branch transaction id';
COMMENT ON COLUMN public.undo_log.xid IS 'global transaction id';
COMMENT ON COLUMN public.undo_log.context IS 'undo_log context,such as serialization';
COMMENT ON COLUMN public.undo_log.rollback_info IS 'rollback info';
COMMENT ON COLUMN public.undo_log.log_status IS '0:normal status,1:defense status';
COMMENT ON COLUMN public.undo_log.log_created IS 'create datetime';
COMMENT ON COLUMN public.undo_log.log_modified IS 'modify datetime';