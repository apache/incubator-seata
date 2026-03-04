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
CREATE TABLE IF NOT EXISTS undo_log
(
    branch_id     Int64,
    xid           String,
    context       String,
    rollback_info LongBlob,
    log_status    Int32,
    log_created   DateTime64(6),
    log_modified  DateTime64(6)
) ENGINE = MergeTree()
ORDER BY (xid, branch_id)
PRIMARY KEY (xid, branch_id)
COMMENT 'AT transaction mode undo table';
