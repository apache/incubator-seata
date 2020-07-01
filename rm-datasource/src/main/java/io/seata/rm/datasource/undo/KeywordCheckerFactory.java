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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Keyword checker factory.
 *
 * @author Wu
 */
public class KeywordCheckerFactory {

    private static final Map<String, KeywordChecker> KEYWORD_CHECKER_MAP = new ConcurrentHashMap<>();

    /**
     * get keyword checker
     *
     * @param dbType the db type
     * @return keyword checker
     */
    public static KeywordChecker getKeywordChecker(String dbType) {
        if (KEYWORD_CHECKER_MAP.get(dbType) != null) {
            return KEYWORD_CHECKER_MAP.get(dbType);
        }
        KeywordChecker tableMetaCache = EnhancedServiceLoader.load(KeywordChecker.class, dbType);
        KEYWORD_CHECKER_MAP.putIfAbsent(dbType, tableMetaCache);
        return tableMetaCache;
    }
}
