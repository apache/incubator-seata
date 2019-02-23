/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.sql;

/**
 * The interface Sql recognizer.
 */
public interface SQLRecognizer {

    /**
     * Type of the SQL. INSERT/UPDATE/DELETE ...
     *
     * @return sql type
     */
    SQLType getSQLType();

    /**
     * TableRecords source related in the SQL, including alias if any.
     * SELECT id, name FROM user u WHERE ...
     * Alias should be 'u' for this SQL.
     *
     * @return table source.
     */
    String getTableAlias();

    /**
     * TableRecords name related in the SQL.
     * SELECT id, name FROM user u WHERE ...
     * TableRecords name should be 'user' for this SQL, without alias 'u'.
     *
     * @return table name.
     * @see #getTableAlias() #getTableAlias()
     */
    String getTableName();

    /**
     * Return the original SQL input by the upper application.
     *
     * @return The original SQL.
     */
    String getOriginalSQL();
}
