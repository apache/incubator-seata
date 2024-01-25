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
package org.apache.seata.core.context;

import io.netty.util.concurrent.FastThreadLocal;
import org.apache.seata.common.loader.LoadLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Fast Thread local context core.
 *
 */
@LoadLevel(name = "FastThreadLocalContextCore", order = Integer.MIN_VALUE + 1)
public class FastThreadLocalContextCore implements ContextCore {

    private FastThreadLocal<Map<String, Object>> fastThreadLocal = new FastThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    @Override
    public Object put(String key, Object value) {
        return fastThreadLocal.get().put(key, value);
    }

    @Override
    public Object get(String key) {
        return fastThreadLocal.get().get(key);
    }

    @Override
    public Object remove(String key) {
        return fastThreadLocal.get().remove(key);
    }

    @Override
    public Map<String, Object> entries() {
        return fastThreadLocal.get();
    }
}
