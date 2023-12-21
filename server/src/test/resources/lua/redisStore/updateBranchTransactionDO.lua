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
-- KEYS[1] branchKey
-- KEYS[2] REDIS_KEY_BRANCH_STATUS
-- KEYS[3] REDIS_KEY_BRANCH_GMT_MODIFIED
-- KEYS[4] REDIS_KEY_BRANCH_APPLICATION_DATA
-- ARGV[1] branchStatus
-- ARGV[2] nowTime
-- ARGV[3] applicationData

-- init data
local branchKey = KEYS[1];
local REDIS_KEY_BRANCH_STATUS = KEYS[2];
local REDIS_KEY_BRANCH_GMT_MODIFIED = KEYS[3];
local REDIS_KEY_BRANCH_APPLICATION_DATA = KEYS[4];
local branchStatus = ARGV[1];
local nowTime = ARGV[2];
local applicationData = ARGV[3];
local result = {};

local previousBranchStatus = redis.call('HGET', branchKey, REDIS_KEY_BRANCH_STATUS);
if (not previousBranchStatus or string.len(tostring(previousBranchStatus)) == 0)
then
    result['success'] = false;
    result['status'] = '';
    result['data'] = '';
    return cjson.encode(result);
end

redis.call('HSET', branchKey, REDIS_KEY_BRANCH_STATUS, branchStatus);
redis.call('HSET', branchKey, REDIS_KEY_BRANCH_GMT_MODIFIED, nowTime);
if (applicationData and string.len(tostring(applicationData)) ~= 0) then
    redis.call('HSET', branchKey, REDIS_KEY_BRANCH_APPLICATION_DATA, applicationData);
end

result['success'] = true;
result['status'] = '';
result['data'] = '';
return cjson.encode(result);