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

import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLDeleteRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLInsertRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLUpdateRecognizer;

public class SQLVisitorFactory {

    public static SQLRecognizer get(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (asts == null || asts.size() != 1) {
            throw new UnsupportedOperationException("xxx");
        }
        SQLRecognizer recognizer = null;
        SQLStatement ast = asts.get(0);
        if (JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            if (ast instanceof SQLInsertStatement) {
                recognizer = new MySQLInsertRecognizer(sql, ast);
            } else if (ast instanceof SQLUpdateStatement) {
                recognizer = new MySQLUpdateRecognizer(sql, ast);
            } else if (ast instanceof SQLDeleteStatement) {
                recognizer = new MySQLDeleteRecognizer(sql, ast);
            } else if (ast instanceof SQLSelectStatement) {
                recognizer = new MySQLSelectForUpdateRecognizer(sql, ast);
            }
        } else {
            throw new UnsupportedOperationException("Just support MySQL by now!");
        }
        return recognizer;
    }

    public static void main(String[] args) throws Throwable {
        String sql = "update material_inventory SET version = ? WHERE is_deleted = 0 and id = ? and version = ?";

        for (int i=0; i<100; i++) {
            long begin = System.currentTimeMillis();
            SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
            long end = System.currentTimeMillis();
            System.out.println(String.format("SQL:[%s] parser too slow, cost [%d] ms.", sql, (end - begin)));
        }

    }
}
