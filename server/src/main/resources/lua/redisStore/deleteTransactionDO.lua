-- Copyright 1999-2019 Seata.io Group.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- User: conghuhu
-- Date: 2022/7/25

-- param description
-- KEYS[1] branchOrGlobalKey
-- KEYS[2] listKey
-- KEYS[3] REDIS_KEY_BRANCH_XID/REDIS_KEY_GLOBAL_XID
-- KEYS[4] REDIS_SEATA_BEGIN_TRANSACTIONS_KEY (only type is global)
-- ARGV[1] type: global or branch
-- ARGV[2] globalTransactionDO xid (only type is global)
-- ARGV[3] globalTransactionDO status (only type is global)

-- init data
local branchOrGlobalKey = KEYS[1];
local listKey = KEYS[2];
local redisKeyXID = KEYS[3];

local type = ARGV[1];

local existedXid = redis.call('HGET', branchOrGlobalKey, redisKeyXID);

if (not existedXid or string.len(tostring(existedXid)) == 0)
then
    return 'true';
end

if (type == 'branch') then
    redis.call('LREM', listKey, 0, branchOrGlobalKey);
    redis.call('DEL', branchOrGlobalKey);
elseif (type == 'global') then
    local xid = ARGV[2];
    local status = tonumber(ARGV[3]);
    local REDIS_SEATA_BEGIN_TRANSACTIONS_KEY = KEYS[4];
    redis.call('LREM', listKey, 0, xid);
    redis.call('DEL', branchOrGlobalKey);
    -- GlobalStatus.Begin or GlobalStatus.UnKnown
    if (status == 1 or status == 0) then
        redis.call('ZREM', REDIS_SEATA_BEGIN_TRANSACTIONS_KEY, branchOrGlobalKey);
    end
end

return 'true';
