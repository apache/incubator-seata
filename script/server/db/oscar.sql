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
CREATE TABLE global_table
(
    XID                       VARCHAR2(128) NOT NULL,
    TRANSACTION_ID            NUMBER(19),
    STATUS                    NUMBER(3)     NOT NULL,
    APPLICATION_ID            VARCHAR2(32),
    TRANSACTION_SERVICE_GROUP VARCHAR2(32),
    TRANSACTION_NAME          VARCHAR2(128),
    TIMEOUT                   NUMBER(10),
    BEGIN_TIME                NUMBER(19),
    APPLICATION_DATA          VARCHAR2(2000),
    GMT_CREATE                TIMESTAMP(0),
    GMT_MODIFIED              TIMESTAMP(0),
    PRIMARY KEY (XID)
);

CREATE INDEX idx_status_gmt_modified ON global_table (STATUS, GMT_MODIFIED);
CREATE INDEX idx_transaction_id ON global_table (TRANSACTION_ID);

-- the table to store BranchSession data
CREATE TABLE branch_table
(
    BRANCH_ID         NUMBER(19)    NOT NULL,
    XID               VARCHAR2(128) NOT NULL,
    TRANSACTION_ID    NUMBER(19),
    RESOURCE_GROUP_ID VARCHAR2(32),
    RESOURCE_ID       VARCHAR2(256),
    BRANCH_TYPE       VARCHAR2(8),
    STATUS            NUMBER(3),
    CLIENT_ID         VARCHAR2(64),
    APPLICATION_DATA  VARCHAR2(2000),
    GMT_CREATE        TIMESTAMP(6),
    GMT_MODIFIED      TIMESTAMP(6),
    PRIMARY KEY (BRANCH_ID)
);

CREATE INDEX idx_xid ON branch_table (XID);

-- the table to store lock data
CREATE TABLE lock_table
(
    ROW_KEY        VARCHAR2(128) NOT NULL,
    XID            VARCHAR2(128),
    TRANSACTION_ID NUMBER(19),
    BRANCH_ID      NUMBER(19)    NOT NULL,
    RESOURCE_ID    VARCHAR2(256),
    TABLE_NAME     VARCHAR2(32),
    PK             VARCHAR2(36),
    STATUS         NUMBER(3)   DEFAULT 0 NOT NULL,
    GMT_CREATE     TIMESTAMP(0),
    GMT_MODIFIED   TIMESTAMP(0),
    PRIMARY KEY (ROW_KEY)
);

comment on column lock_table.STATUS is '0:locked ,1:rollbacking';
CREATE INDEX idx_branch_id ON lock_table (BRANCH_ID);
CREATE INDEX idx_lock_table_xid ON lock_table (XID);
CREATE INDEX idx_status ON lock_table (STATUS);

CREATE TABLE distributed_lock (
                                  LOCK_KEY     VARCHAR2(20)  NOT NULL,
                                  LOCK_VALUE        VARCHAR2(20)  NOT NULL,
                                  EXPIRE       DECIMAL(18)   NOT NULL,
                                  PRIMARY KEY (LOCK_KEY)
);

INSERT INTO distributed_lock (LOCK_KEY, LOCK_VALUE, EXPIRE) VALUES ('AsyncCommitting', ' ', 0);
INSERT INTO distributed_lock (LOCK_KEY, LOCK_VALUE, EXPIRE) VALUES ('RetryCommitting', ' ', 0);
INSERT INTO distributed_lock (LOCK_KEY, LOCK_VALUE, EXPIRE) VALUES ('RetryRollbacking', ' ', 0);
INSERT INTO distributed_lock (LOCK_KEY, LOCK_VALUE, EXPIRE) VALUES ('TxTimeoutCheck', ' ', 0);
CREATE TABLE VGROUP_TABLE
(
    VGROUP    VARCHAR2(255) PRIMARY KEY,
    NAMESPACE VARCHAR2(255),
    CLUSTER   VARCHAR2(255)
);