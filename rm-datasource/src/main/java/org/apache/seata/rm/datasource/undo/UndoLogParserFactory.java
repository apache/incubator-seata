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
package org.apache.seata.rm.datasource.undo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;

/**
 * The type Undo log parser factory.
 *
 */
public class UndoLogParserFactory {

    private UndoLogParserFactory() {

    }

    /**
     * {serializerName:UndoLogParser}
     */
    private static final ConcurrentMap<String, UndoLogParser> INSTANCES = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final UndoLogParser INSTANCE = getInstance(UndoLogConstants.DEFAULT_SERIALIZER);
    }

    /**
     * Gets default UndoLogParser instance.
     *
     * @return the instance
     */
    public static UndoLogParser getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Gets UndoLogParser by name
     *
     * @param name parser name
     * @return the UndoLogParser
     */
    public static UndoLogParser getInstance(String name) {
        if (StringUtils.equalsIgnoreCase("fst", name)) {
            throw new IllegalArgumentException(
                "Since fst is no longer maintained, this serialization extension has been removed from version 2.0 for security and stability reasons.");
        }
        return CollectionUtils.computeIfAbsent(INSTANCES, name,
            key -> EnhancedServiceLoader.load(UndoLogParser.class, name));
    }
}
