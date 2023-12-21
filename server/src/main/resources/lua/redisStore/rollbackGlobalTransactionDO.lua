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
-- KEYS[2] REDIS_KEY_GLOBAL_XID
-- KEYS[3] REDIS_KEY_GLOBAL_STATUS
-- KEYS[4] REDIS_KEY_GLOBAL_GMT_MODIFIED
-- KEYS[5] status
-- ARGV[1] previousStatus
-- ARGV[2] previousGmtModified
-- ARGV[3] xid
-- ARGV[4] hmset
-- ARGV[5] lrem
-- ARGV[6] rpush

-- init data
local globalKey = KEYS[1];
local REDIS_KEY_GLOBAL_XID = KEYS[2];
local REDIS_KEY_GLOBAL_STATUS = KEYS[3];
local REDIS_KEY_GLOBAL_GMT_MODIFIED = KEYS[4];
local status = KEYS[5];
local previousStatus = ARGV[1];
local previousGmtModified = ARGV[2];
local xid = ARGV[3];
local hmset = ARGV[4];
local lrem = ARGV[5];
local rpush = ARGV[6];

if string.upper(hmset) == "OK" then
    local xid2 = redis.call('HGET', globalKey, REDIS_KEY_GLOBAL_XID);
    if (xid2 and string.len(tostring(xid2)) ~= 0) then
        redis.call('HMSET', globalKey, REDIS_KEY_GLOBAL_STATUS, previousStatus, REDIS_KEY_GLOBAL_GMT_MODIFIED, previousGmtModified);
    end
end

if tonumber(lrem) > 0 then
    redis.call('RPUSH', 'SEATA_STATUS_' .. previousStatus, xid);
end

if tonumber(rpush) then
    redis.call('LREM', 'SEATA_STATUS_' .. status, 0, xid);
end