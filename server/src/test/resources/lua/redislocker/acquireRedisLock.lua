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

-- User: tianyu.li;conghuhu
-- Date: 2022/7/1

-- param description
-- ARGV[1] needLockDOs.size()
-- ARGV[2] argSize
-- ARGV[3] needLockXid
-- ARGV[-1] localKeysString
-- KEYS[1 ~ needLockKeys.size()] needLockKeys
-- KEYS[-2] xidLockKey
-- KEYS[-1] branchId

-- init data
local array = {};
local result = {};
local keySize = ARGV[1];
local argSize = ARGV[2];
-- Loop through all keys to see if they can be used , when a key is not available, exit
for i = 1, keySize do
    local needLockKey = KEYS[i]
    -- search lock xid
    local existedLockXid = redis.call('HGET', needLockKey, 'xid');
    -- query lockStatus
    local status = redis.call('HGET', needLockKey, 'status');

    -- if a global lock is found in the Rollbacking state,the fail-fast code is returned directly
    if (status == '1' or tonumber(status) == 1)
    then
        result["success"] = false
        result['status'] = 'AnotherRollbackIng'
        result["data"] = existedLockXid
        return cjson.encode(result)
    end

    -- if lock xid is nil
    if (not existedLockXid)
    -- set 'no' mean There is need to store lock information
    then
        array[i] = 'no'
    else
        if (existedLockXid ~= ARGV[3])
        then
            -- return fail
            result['success'] = false
            result['status'] = 'AnotherHoldIng'
            result["data"] = existedLockXid
            return cjson.encode(result)
        else
            -- set 'yes' mean  There is not need to store lock information
            array[i] = 'yes'
        end
    end
end
-- Loop through array
for i = 1, keySize do
    -- if is no ,The lock information is stored
    if (array[i] == 'no')
    then
        -- set xid
        redis.call('HSET', KEYS[i], 'xid', ARGV[3]);
        -- set transactionId
        redis.call('HSET', KEYS[i], 'transactionId', ARGV[(i - 1) * 6 + 4]);
        -- set branchId
        redis.call('HSET', KEYS[i], 'branchId', ARGV[(i - 1) * 6 + 5]);
        -- set resourceId
        redis.call('HSET', KEYS[i], 'resourceId', ARGV[(i - 1) * 6 + 6]);
        -- set tableName
        redis.call('HSET', KEYS[i], 'tableName', ARGV[(i - 1) * 6 + 7]);
        -- set rowKey
        redis.call('HSET', KEYS[i], 'rowKey', ARGV[(i - 1) * 6 + 8]);
        -- set pk
        redis.call('HSET', KEYS[i], 'pk', ARGV[(i - 1) * 6 + 9]);
        -- exit if
    end
    -- exit for
end
-- set SEATA_GLOBAL_LOCK
redis.call('HSET', KEYS[(keySize + 1)], KEYS[(keySize + 2)], ARGV[(argSize + 0)]);

--  return success
result['success'] = true
result['status'] = 'GetLock'
result['data'] = ARGV[3]
return cjson.encode(result)
