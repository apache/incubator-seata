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

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Initializes fastjson2 object readers before concurrent deserialization begins.
 */
public final class Fastjson2ObjectReaderWarmup {

    private static final Lock WARMUP_LOCK = new ReentrantLock();

    private Fastjson2ObjectReaderWarmup() {}

    public static void warmup(Iterable<Class<?>> types) {
        WARMUP_LOCK.lock();
        try {
            ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
            for (Class<?> type : types) {
                provider.getObjectReader(type, true);
            }
        } finally {
            WARMUP_LOCK.unlock();
        }
    }

    public static void warmup(Class<?>... types) {
        warmup(Arrays.asList(types));
    }
}
