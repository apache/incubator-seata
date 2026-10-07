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




-- Step 1: add new column
ALTER TABLE vgroup_table
ADD COLUMN cluster_name VARCHAR2(255);

-- Step 2: copy existing data
UPDATE vgroup_table
SET cluster_name = cluster;

-- Step 3: drop old constraint
ALTER TABLE vgroup_table
DROP CONSTRAINT uk_vgroup_namespace_cluster;

-- Step 4: drop old column
ALTER TABLE vgroup_table
DROP COLUMN cluster;

-- Step 5: add new constraint
ALTER TABLE vgroup_table
ADD CONSTRAINT uk_vgroup_namespace_cluster_name
UNIQUE (vGroup, namespace, cluster_name);
