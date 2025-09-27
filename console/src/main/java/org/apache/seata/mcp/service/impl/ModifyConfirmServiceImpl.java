/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.service.ModifyConfirmService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModifyConfirmServiceImpl implements ModifyConfirmService {

    private static final Map<String, Long> MODIFY_KEY = new ConcurrentHashMap<>();

    private static final long EXPIRE_MS = 60_000; // Key timeout period

    @Override
    public Map<String, String> confirmAndGetKey() {
        String key = UUID.randomUUID().toString();
        MODIFY_KEY.put(key, System.currentTimeMillis());
        Map<String, String> map = new HashMap<>();
        map.put("modify_key", key);
        map.put("expire_time", "60s");
        map.put(
                "Important!!!",
                "You need to repeat the content to be modified by the user and get confirmation from the user before you can continue to call the modification tool");
        return map;
    }

    @Override
    public Boolean isValidKey(String key) {
        Long ts = MODIFY_KEY.get(key);
        if (ts == null || (System.currentTimeMillis() - ts) > EXPIRE_MS) {
            MODIFY_KEY.remove(key);
            return false;
        }
        MODIFY_KEY.remove(key); // Delete when you're done with it
        return true;
    }
}
