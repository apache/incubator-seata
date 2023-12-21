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
-- Date: 2022/7/27

-- param description
-- KEYS[1] globalKey
-- KEYS[2] REDIS_KEY_GLOBAL_STATUS
-- KEYS[3] REDIS_KEY_GLOBAL_GMT_MODIFIED
-- KEYS[4] REDIS_SEATA_BEGIN_TRANSACTIONS_KEY
-- ARGV[1] status
-- ARGV[2] nowTime
-- ARGV[3] xid

-- init data
local globalKey = KEYS[1];
local REDIS_KEY_GLOBAL_STATUS = KEYS[2];
local REDIS_KEY_GLOBAL_GMT_MODIFIED = KEYS[3];
local REDIS_SEATA_BEGIN_TRANSACTIONS_KEY = KEYS[4];

local status = ARGV[1];
local nowTime = ARGV[2];
local xid = ARGV[3];

local result = {};

-- is timeout global status
local function isTimeoutGlobalStatus(s)
    local globalStatus = tonumber(s);
    return globalStatus == 13 or globalStatus == 14 or globalStatus == 6 or globalStatus == 7;
end

-- is rollback global status
local function isRollbackGlobalStatus(s)
    local globalStatus = tonumber(s);
    return globalStatus == 4 or globalStatus == 5 or globalStatus == 11 or globalStatus == 12 or globalStatus == 17;
end

local function isCommitGlobalStatus(s)
    local globalStatus = tonumber(s);
    return globalStatus == 2 or globalStatus == 8 or globalStatus == 3 or globalStatus == 9 or globalStatus == 10 or globalStatus == 16;
end

-- check the relation of before status and after status
local function validateUpdateStatus(before, after)
    if isTimeoutGlobalStatus(before) and isCommitGlobalStatus(after) then
        return false;
    end
    if isCommitGlobalStatus(before) and isTimeoutGlobalStatus(after) then
        return false;
    end
    if isRollbackGlobalStatus(before) and isCommitGlobalStatus(after) then
        return false;
    end
    if isCommitGlobalStatus(before) and isRollbackGlobalStatus(after) then
        return false;
    end
    return true;
end

local statusAndGmtModified = redis.call('HMGET', globalKey, REDIS_KEY_GLOBAL_STATUS, REDIS_KEY_GLOBAL_GMT_MODIFIED);
local previousStatus = statusAndGmtModified[1];
local previousGmtModified = statusAndGmtModified[2];

if (not previousStatus and string.len(tostring(previousStatus)) ~= 0) then
    result['success'] = false;
    result['status'] = 'NotExisted';
    result['data'] = '';
    return cjson.encode(result);
end

if previousStatus == status then
    result['success'] = true;
    result['status'] = '';
    result['data'] = '';
    return cjson.encode(result);
end

if not validateUpdateStatus(previousStatus, status) then
    result['success'] = false;
    result['status'] = 'ChangeStatusFail';
    result['data'] = previousGmtModified;
    return cjson.encode(result);
end

local data = {};
data[1] = redis.call('HMSET', globalKey, REDIS_KEY_GLOBAL_STATUS, status, REDIS_KEY_GLOBAL_GMT_MODIFIED, nowTime)['ok'];
data[2] = tostring(redis.call('LREM', 'SEATA_STATUS_' .. previousStatus, 0, xid));
data[3] = tostring(redis.call('RPUSH', 'SEATA_STATUS_' .. status, xid));
data[4] = tostring(redis.call('ZREM', REDIS_SEATA_BEGIN_TRANSACTIONS_KEY, globalKey));
data[5] = previousStatus;
data[6] = previousGmtModified;

result['success'] = true;
result['status'] = '';
result['data'] = cjson.encode(data);
return cjson.encode(result);