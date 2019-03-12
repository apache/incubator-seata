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
package com.alibaba.fescar.rm.datasource.sql.druid;

import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

/**
 * The type Abstract my sql recognizer test.
 *
 * @author hanwen created at 2019-01-25
 */
public class AbstractMySQLRecognizerTest {

    /**
     * Gets sql statement.
     *
     * @param sql the sql
     * @return the sql statement
     */
    public SQLStatement getSQLStatement(String sql) {
        List<SQLStatement> stats = SQLUtils.parseStatements(sql, "mysql");
        return stats.get(0);
    }

}
