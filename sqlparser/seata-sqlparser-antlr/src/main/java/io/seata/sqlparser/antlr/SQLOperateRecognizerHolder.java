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
package io.seata.sqlparser.antlr;

import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.antlr.mysql.MySqlContext;

/**
 * The interface SQLOperateRecognizerHolder
 *
 * @author zhihou
 */
public interface SQLOperateRecognizerHolder {

    /**
     * Get delete recognizer
     *
     * @param sql the sql
     * @return the delete recognizer
     */
    SQLRecognizer getDeleteRecognizer(MySqlContext mySqlContext,String sql);

    /**
     * Get insert recognizer
     *
     * @param sql the sql
     * @return the insert recognizer
     */
    SQLRecognizer getInsertRecognizer(MySqlContext mySqlContext,String sql);

    /**
     * Get update recognizer
     *
     * @param sql the sql
     * @return the update recognizer
     */
    SQLRecognizer getUpdateRecognizer(MySqlContext mySqlContext,String sql);

    /**
     * Get SelectForUpdate recognizer
     *
     * @param sql the sql
     * @return the SelectForUpdate recognizer
     */
    SQLRecognizer getSelectForUpdateRecognizer(MySqlContext mySqlContext,String sql);

}