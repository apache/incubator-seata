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

import java.util.List;

/**
 * @author jingliu_xiong@foxmail.com
 */
public interface SQLReplaceRecognizer extends SQLRecognizer {
    /**
     * select query is empty.
     *
     * @return true: empty. false: not empty.
     */
    boolean selectQueryIsEmpty();

    /**
     * get replace columns.
     *
     * @return
     */
    List<String> getReplaceColumns();

    /**
     * get replace values.
     *
     * @return (?, ?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?, ?)
     */
    List<String> getReplaceValues();

    /**
     * get select query
     *
     * @return if do not have select query, return ""
     */
    String getSelectQuery();
}
