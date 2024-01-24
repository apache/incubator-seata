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
package org.apache.seata.sqlparser;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Keyword checker factory.
 *
 */
public class EscapeHandlerFactory {

    private static final Map<String, EscapeHandler> ESCAPE_HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * get keyword checker
     *
     * @param dbType the db type
     * @return keyword checker
     */
    public static EscapeHandler getEscapeHandler(String dbType) {
        return CollectionUtils.computeIfAbsent(ESCAPE_HANDLER_MAP, dbType,
            key -> EnhancedServiceLoader.load(EscapeHandler.class, dbType));
    }
}
