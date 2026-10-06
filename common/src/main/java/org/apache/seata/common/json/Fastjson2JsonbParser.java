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
package org.apache.seata.common.json;

import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.filter.Filter;

/**
 * Serializes Seata JSONB decoding while fastjson2 shares mutable reference-resolution state.
 * Reader warmup alone does not protect the cached reference paths used during parsing.
 */
public final class Fastjson2JsonbParser {

    private Fastjson2JsonbParser() {}

    // Both entry points must use the same monitor: RPC and undo-log parsers share fastjson2 caches.
    // Keep the entire parse guarded, including the deferred reference-resolution phase.
    public static synchronized <T> T parseObject(byte[] bytes, Class<T> type, JSONReader.Feature... features) {
        return JSONB.parseObject(bytes, type, features);
    }

    public static synchronized <T> T parseObject(
            byte[] bytes, Class<T> type, Filter filter, JSONReader.Feature... features) {
        return JSONB.parseObject(bytes, type, filter, features);
    }
}
