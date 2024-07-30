--
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

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
    [status]            tinyint       NULL,
    [gmt_create]     datetime2     NULL,
    [gmt_modified]   datetime2     NULL,
    PRIMARY KEY CLUSTERED ([row_key])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)
GO

CREATE NONCLUSTERED INDEX [idx_status]
    ON [lock_table] (
                     [status]
        )
GO

CREATE NONCLUSTERED INDEX [idx_branch_id]
    ON [lock_table] (
                     [branch_id]
        )
GO

-- the table to store distributed lock constants
CREATE TABLE [distributed_lock]
(
    [lock_key]   char(20)    not null primary key,
    [lock_value] varchar(20) not null,
    [expire]     bigint
    )
GO

INSERT INTO [distributed_lock] (lock_key, lock_value, expire) VALUES ('AsyncCommitting', ' ', 0);
INSERT INTO [distributed_lock] (lock_key, lock_value, expire) VALUES ('RetryCommitting', ' ', 0);
INSERT INTO [distributed_lock] (lock_key, lock_value, expire) VALUES ('RetryRollbacking', ' ', 0);
INSERT INTO [distributed_lock] (lock_key, lock_value, expire) VALUES ('TxTimeoutCheck', ' ', 0);
INSERT INTO [distributed_lock] (lock_key, lock_value, expire) VALUES ('UndologDelete', ' ', 0);

CREATE TABLE [vgroup_table]
(
    [vGroup]    nvarchar(255) NOT NULL,
    [namespace] nvarchar(255) NOT NULL,
    [cluster]   nvarchar(255) NOT NULL,
    PRIMARY KEY CLUSTERED ([vGroup])
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
)