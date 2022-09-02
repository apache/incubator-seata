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
package io.seata.rm.datasource.exec.handler;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lyx
 */
public class AfterHandlerFactory {

    private static final Map<String, AfterHandler> AFTER_HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * get after handler
     *
     * @param sqlType the SQL type
     * @return AfterHandler
     */
    public static AfterHandler getAfterHandler(String sqlType) {
        return CollectionUtils.computeIfAbsent(AFTER_HANDLER_MAP, sqlType,
            key -> EnhancedServiceLoader.load(AfterHandler.class, sqlType));
    }
}
