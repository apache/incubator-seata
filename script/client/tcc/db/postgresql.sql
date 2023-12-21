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

-- -------------------------------- The script used for tcc fence  --------------------------------
CREATE TABLE IF NOT EXISTS public.tcc_fence_log
(
    xid              VARCHAR(128)  NOT NULL,
    branch_id        BIGINT        NOT NULL,
    action_name      VARCHAR(64)   NOT NULL,
    status           SMALLINT      NOT NULL,
    gmt_create       TIMESTAMP(3)  NOT NULL,
    gmt_modified     TIMESTAMP(3)  NOT NULL,
    CONSTRAINT pk_tcc_fence_log PRIMARY KEY (xid, branch_id)
);
CREATE INDEX idx_gmt_modified ON public.tcc_fence_log (gmt_modified);
CREATE INDEX idx_status ON public.tcc_fence_log (status);