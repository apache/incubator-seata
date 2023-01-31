-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE global_table
(
    xid                       VARCHAR2(128) NOT NULL,
    transaction_id            NUMBER(19),
    status                    NUMBER(3)     NOT NULL,
    application_id            VARCHAR2(32),
    transaction_service_group VARCHAR2(32),
    transaction_name          VARCHAR2(128),
    timeout                   NUMBER(10),
    begin_time                NUMBER(19),
    application_data          VARCHAR2(2000),
    gmt_create                TIMESTAMP(0),
    gmt_modified              TIMESTAMP(0),
    PRIMARY KEY (xid)
);

CREATE INDEX idx_status_gmt_modified ON global_table (status, gmt_modified);
CREATE INDEX idx_transaction_id ON global_table (transaction_id);

-- the table to store BranchSession data
CREATE TABLE branch_table
(
    branch_id         NUMBER(19)    NOT NULL,
    xid               VARCHAR2(128) NOT NULL,
    transaction_id    NUMBER(19),
    resource_group_id VARCHAR2(32),
    resource_id       VARCHAR2(256),
    branch_type       VARCHAR2(8),
    status            NUMBER(3),
    client_id         VARCHAR2(64),
    application_data  VARCHAR2(2000),
    gmt_create        TIMESTAMP(6),
    gmt_modified      TIMESTAMP(6),
    PRIMARY KEY (branch_id)
);

CREATE INDEX idx_xid ON branch_table (xid);

-- the table to store lock data
CREATE TABLE lock_table
(
    row_key        VARCHAR2(128) NOT NULL,
    xid            VARCHAR2(128),
    transaction_id NUMBER(19),
    branch_id      NUMBER(19)    NOT NULL,
    resource_id    VARCHAR2(256),
    table_name     VARCHAR2(32),
    pk             VARCHAR2(36),
    status         NUMBER(3)   DEFAULT 0 NOT NULL,
    gmt_create     TIMESTAMP(0),
    gmt_modified   TIMESTAMP(0),
    PRIMARY KEY (row_key)
);

comment on column lock_table.status is '0:locked ,1:rollbacking';
CREATE INDEX idx_branch_id ON lock_table (branch_id);
CREATE INDEX idx_lock_table_xid ON lock_table (xid);
CREATE INDEX idx_status ON lock_table (status);

CREATE TABLE distributed_lock (
    lock_key     VARCHAR2(20)  NOT NULL,
    lock_value        VARCHAR2(20)  NOT NULL,
    expire       DECIMAL(18)   NOT NULL,
    PRIMARY KEY (lock_key)
);

INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('AsyncCommitting', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('RetryCommitting', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('RetryRollbacking', ' ', 0);
INSERT INTO distributed_lock (lock_key, lock_value, expire) VALUES ('TxTimeoutCheck', ' ', 0);
