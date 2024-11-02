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
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE UNDO_LOG
(
    ID numeric(19,0) NOT NULL,
    BRANCH_ID numeric(19,0) NOT NULL,
    XID character varying(128) NOT NULL,
    "CONTEXT" character varying(128) NOT NULL,
    ROLLBACK_INFO blob NOT NULL,
    LOG_STATUS numeric(10,0) NOT NULL,
    LOG_CREATED timestamp(0) without time zone NOT NULL,
    LOG_MODIFIED timestamp(0) without time zone NOT NULL,
    CONSTRAINT UNDO_LOG_PKEY PRIMARY KEY (ID),
    CONSTRAINT UX_UNDO_LOG UNIQUE (XID, BRANCH_ID)
);

CREATE INDEX ix_log_created ON UNDO_LOG(LOG_CREATED);
COMMENT ON TABLE UNDO_LOG IS 'AT transaction mode undo table';
COMMENT ON COLUMN UNDO_LOG.BRANCH_ID is 'branch transaction id';
COMMENT ON COLUMN UNDO_LOG.XID is 'global transaction id';
COMMENT ON COLUMN UNDO_LOG.CONTEXT is 'undo_log context,such as serialization';
COMMENT ON COLUMN UNDO_LOG.ROLLBACK_INFO is 'rollback info';
COMMENT ON COLUMN UNDO_LOG.LOG_STATUS is '0:normal status,1:defense status';
COMMENT ON COLUMN UNDO_LOG.LOG_CREATED is 'create datetime';
COMMENT ON COLUMN UNDO_LOG.LOG_MODIFIED is 'modify datetime';

-- Generate ID using sequence and trigger
CREATE SEQUENCE UNDO_LOG_SEQ START WITH 1 INCREMENT BY 1;