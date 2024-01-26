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
package org.apache.seata.rm.datasource.exec.mariadb;

import org.apache.seata.rm.datasource.exec.mysql.MySQLInsertExecutor;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * The type Mariadb insert executor.
 *
 */
@LoadLevel(name = JdbcConstants.MARIADB, scope = Scope.PROTOTYPE)
public class MariadbInsertExecutor extends MySQLInsertExecutor {

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public MariadbInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
        SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

}
