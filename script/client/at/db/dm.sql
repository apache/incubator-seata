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

CREATE TABLE IF NOT EXISTS "UNDO_LOG"
(
    "ID"            BIGINT IDENTITY(1, 1) NOT NULL,
    "BRANCH_ID"     BIGINT       NOT NULL,
    "XID"           VARCHAR(128) NOT NULL,
    "CONTEXT"       VARCHAR(128) NOT NULL,
    "ROLLBACK_INFO" BLOB         NOT NULL,
    "LOG_STATUS"    INT          NOT NULL,
    "LOG_CREATED"   TIMESTAMP(0) NOT NULL,
    "LOG_MODIFIED"  TIMESTAMP(0) NOT NULL,
    "EXT" VARCHAR(100),
    NOT CLUSTER PRIMARY KEY("ID"),
    CONSTRAINT "UX_UNDO_LOG" UNIQUE("XID", "BRANCH_ID")
) STORAGE (ON "MAIN", CLUSTERBTR);

CREATE UNIQUE INDEX "PRIMARY" ON "UNDO_LOG"("ID" ASC) STORAGE (ON "MAIN", CLUSTERBTR);

COMMENT ON TABLE "UNDO_LOG" IS 'AT transaction mode undo table';
