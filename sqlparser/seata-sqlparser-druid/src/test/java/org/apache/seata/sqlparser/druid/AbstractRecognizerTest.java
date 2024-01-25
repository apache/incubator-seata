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
package org.apache.seata.sqlparser.druid;

import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

/**
 * The type Abstract my sql recognizer test.
 *
 */
public abstract class AbstractRecognizerTest {

    /**
     * Gets sql statement.
     *
     * @param sql the sql
     * @return the sql statement
     */
    public SQLStatement getSQLStatement(String sql) {
        List<SQLStatement> stats = SQLUtils.parseStatements(sql, getDbType());
        return stats.get(0);
    }

    public abstract String getDbType();

}
