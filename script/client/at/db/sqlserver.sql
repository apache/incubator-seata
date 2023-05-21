-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE [undo_log]
(
    [branch_id]     bigint                NOT NULL,
    [xid]           varchar(128)          NOT NULL,
    [context]       varchar(128)          NOT NULL,
    [rollback_info] varbinary(max)        NOT NULL,
    [log_status]    int                   NOT NULL,
    [log_created]   datetime2              NOT NULL,
    [log_modified]  datetime2              NOT NULL,
    CONSTRAINT [ux_undo_log] UNIQUE NONCLUSTERED ([xid], [branch_id])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO