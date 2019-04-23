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

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.undo.mysql.keyword.MySQLKeywordChecker;

/**
 * The type Keyword checker factory.
 *
 * @author Wu
 * @date 2019 /3/5 The Type keyword checker factory
 */
public class KeywordCheckerFactory {

    /**
     * get keyword checker
     *
     * @param dbType the db type
     * @return keyword checker
     */
    public static KeywordChecker getKeywordChecker(String dbType) {
        if (dbType.equals(JdbcConstants.MYSQL)) {
            return MySQLKeywordChecker.getInstance();
        } else {
            throw new NotSupportYetException(dbType);
        }

    }
}
