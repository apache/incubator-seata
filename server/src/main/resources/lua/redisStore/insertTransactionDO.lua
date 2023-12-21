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

-- User: conghuhu
-- Date: 2022/7/25

-- param description
-- KEYS[1] branchOrGlobalKey
-- KEYS[2] listKey
-- KEYS[3~-2] transactionDOMap.keys
-- KEYS[-1] REDIS_SEATA_BEGIN_TRANSACTIONS_KEY (only type is global)
-- ARGV[1] type: global or branch
-- ARGV[2] transactionDOMap.size()
-- ARGV[3~-2] transactionDOMap.values
-- ARGV[-2] xid (only type is global)
-- ARGV[-1] beginTime+timeout (only type is global)

-- init data
local branchOrGlobalKey = KEYS[1];
local listKey = KEYS[2];

local type = ARGV[1];
local keySize = tonumber(ARGV[2]);

for i = 1, keySize do
    redis.call('HSET', branchOrGlobalKey, KEYS[i + 2], ARGV[i + 2]);
end

if type == 'branch' then
    redis.call('RPUSH', listKey, branchOrGlobalKey);
elseif type == 'global' then
    local REDIS_SEATA_BEGIN_TRANSACTIONS_KEY = KEYS[keySize + 3];
    redis.call('RPUSH', listKey, ARGV[keySize + 3]);
    redis.call('ZADD', REDIS_SEATA_BEGIN_TRANSACTIONS_KEY, ARGV[keySize + 4], branchOrGlobalKey)
end

return 'true';

