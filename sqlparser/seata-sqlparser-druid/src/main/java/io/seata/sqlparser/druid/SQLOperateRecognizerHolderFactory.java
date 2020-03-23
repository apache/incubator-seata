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
package io.seata.sqlparser.druid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.loader.EnhancedServiceLoader;

/**
 * The SQLOperateRecognizerHolderFactory
 *
 * @author: Zhibei Hao
 */
public class SQLOperateRecognizerHolderFactory {

    private static final Map<String, SQLOperateRecognizerHolder> RECOGNIZER_HOLDER_MAP = new ConcurrentHashMap<>();

    /**
     * get SQLOperateRecognizer by db type
     *
     * @param dbType the db type
     * @return the SQLOperateRecognizer
     */
    public static SQLOperateRecognizerHolder getSQLRecognizerHolder(String dbType) {
        if (RECOGNIZER_HOLDER_MAP.get(dbType) != null) {
            return RECOGNIZER_HOLDER_MAP.get(dbType);
        }
        SQLOperateRecognizerHolder recognizerHolder = EnhancedServiceLoader.load(SQLOperateRecognizerHolder.class, dbType, SQLOperateRecognizerHolderFactory.class.getClassLoader());
        RECOGNIZER_HOLDER_MAP.putIfAbsent(dbType, recognizerHolder);
        return recognizerHolder;
    }
}
