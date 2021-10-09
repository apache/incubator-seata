-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE [global_table]
(
    [xid]                       nvarchar(128)  NOT NULL,
    [transaction_id]            bigint        NULL,
    [status]                    tinyint       NOT NULL,
    [application_id]            nvarchar(32)   NULL,
    [transaction_service_group] nvarchar(32)   NULL,
    [transaction_name]          nvarchar(128)  NULL,
    [timeout]                   int           NULL,
    [begin_time]                bigint        NULL,
    [application_data]          nvarchar(2000) NULL,
    [gmt_create]                datetime2      NULL,
    [gmt_modified]              datetime2      NULL,
    PRIMARY KEY CLUSTERED ([xid])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO

CREATE NONCLUSTERED INDEX [idx_gmt_modified_status]
    ON [global_table] (
                       [gmt_modified],
                       [status]
        )
GO

CREATE NONCLUSTERED INDEX [idx_transaction_id]
    ON [global_table] (
                       [transaction_id]
        )
GO

-- the table to store BranchSession data
CREATE TABLE [branch_table]
(
    [branch_id]         bigint        NOT NULL,
    [xid]               nvarchar(128)  NOT NULL,
    [transaction_id]    bigint        NULL,
    [resource_group_id] nvarchar(32)   NULL,
    [resource_id]       nvarchar(256)  NULL,
    [branch_type]       varchar(8)    NULL,
    [status]            tinyint       NULL,
    [client_id]         nvarchar(64)   NULL,
    [application_data]  nvarchar(2000) NULL,
    [gmt_create]        datetime2      NULL,
    [gmt_modified]      datetime2      NULL,
    PRIMARY KEY CLUSTERED ([branch_id])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO

CREATE NONCLUSTERED INDEX [idx_xid]
    ON [branch_table] (
                       [xid]
        )
GO

-- the table to store lock data
CREATE TABLE [lock_table]
(
    [row_key]        nvarchar(128) NOT NULL,
    [xid]            nvarchar(128) NULL,
    [transaction_id] bigint       NULL,
    [branch_id]      bigint       NOT NULL,
    [resource_id]    nvarchar(256) NULL,
    [table_name]     nvarchar(32)  NULL,
    [pk]             nvarchar(36)  NULL,
    [gmt_create]     datetime2     NULL,
    [gmt_modified]   datetime2     NULL,
    PRIMARY KEY CLUSTERED ([row_key])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO

CREATE NONCLUSTERED INDEX [idx_branch_id]
    ON [lock_table] (
                     [branch_id]
        )
GO
