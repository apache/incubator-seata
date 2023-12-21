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
-- Date: 2022/7/7

-- param description
-- KEYS[1] lockKeys.size()
-- KEYS[2~lockKeys.size()+1] lockKey
-- ARGV[1] xid

-- init data
local lockKeysSize = tonumber(KEYS[1]);
local xid = tostring(ARGV[1]);
for i = 1, lockKeysSize do
    local lockKey = KEYS[i + 1]
    local existedXid = redis.call('HGET', lockKey, 'xid')
    if (existedXid == nil or xid == tostring(existedXid)) then
        return 'true'
    end
end
return 'false'