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
package io.seata.rm.datasource.sql;

import io.seata.common.loader.EnhancedServiceLoader;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The SQLOperateRecognizerHolderFactory
 *
 * @author: Zhibei Hao
 */
public class SQLOperateRecognizerHolderFactory {

    private static volatile Map<String, SQLOperateRecognizerHolder> recognizerHolderMap;

    /**
     * get SQLOperateRecognizer by db type
     *
     * @param dbType the db type
     * @return the SQLOperateRecognizer
     */
    public static SQLOperateRecognizerHolder getSQLRecognizerHolder(String dbType) {

        if (recognizerHolderMap == null) {
            synchronized (SQLOperateRecognizerHolderFactory.class) {
                if (recognizerHolderMap == null) {
                    Map<String, SQLOperateRecognizerHolder> initializedMap = new HashMap<>();
                    List<SQLOperateRecognizerHolder> holderList = EnhancedServiceLoader.loadAll(
                        SQLOperateRecognizerHolder.class);
                    for (SQLOperateRecognizerHolder holder : holderList) {
                        initializedMap.put(holder.getDbType().toLowerCase(), holder);
                    }
                    recognizerHolderMap = initializedMap;
                }
            }
        }
        if (recognizerHolderMap.containsKey(dbType)) {
            return recognizerHolderMap.get(dbType);
        }
        throw new UnsupportedOperationException(MessageFormat.format("now not support {0}", dbType));
    }
}
