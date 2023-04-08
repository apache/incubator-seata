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
package io.seata.sqlparser;

import java.util.Collection;
import java.util.List;

/**
 * The interface Sql insert recognizer.
 *
 * @author sharajava
 */
public interface SQLInsertRecognizer extends SQLRecognizer {

    /**
     * insert columns is empty.
     * @return true: empty. false: not empty.
     */
    boolean insertColumnsIsEmpty();

    /**
     * Gets insert columns.
     *
     * @return the insert columns
     */
    List<String> getInsertColumns();

    /**
     * Gets insert rows.
     *
     * @param primaryKeyIndex insert sql primary key index.
     * @return the insert rows
     */
    List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex);

    /**
     * Gets insert
     *
     * @return  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
     */
    List<String> getInsertParamsValue();

    /**
     * Gets DuplicateKey columns.
     *
     * @return the duplicateKey columns
     */
    List<String> getDuplicateKeyUpdate();

    /**
     * Gets insert columns is Simplified.
     *
     * @return (`a`, `b`, `c`)  ->  (a, b, c)
     */
    List<String> getInsertColumnsUnEscape();
}
