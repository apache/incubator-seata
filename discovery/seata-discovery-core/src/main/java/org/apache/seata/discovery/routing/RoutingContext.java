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
package org.apache.seata.discovery.routing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains various information needed during routing process
 */
public class RoutingContext {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Set attribute
     * @param key key
     * @param value value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Get attribute
     * @param key key
     * @return value
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
