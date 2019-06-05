/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo;

import io.seata.common.loader.EnhancedServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The type Undo log parser factory.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public class UndoLogParserFactory {

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
        UndoLogParser parser = INSTANCES.get(name);
        if (parser == null) {
            synchronized (UndoLogParserFactory.class) {
                parser = INSTANCES.get(name);
                if (parser == null) {
                    parser = EnhancedServiceLoader.load(UndoLogParser.class, name);
                    INSTANCES.putIfAbsent(name, parser);
                }
            }
        }
        return parser;
    }
}
