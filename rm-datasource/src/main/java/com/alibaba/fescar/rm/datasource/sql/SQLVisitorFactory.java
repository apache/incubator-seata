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

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlHintStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLDeleteRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLInsertRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLUpdateRecognizer;

public class SQLVisitorFactory {

    public static SQLRecognizer get(String sql, String dbType) {
        if (!JdbcConstants.MYSQL.equalsIgnoreCase(dbType)) {
            throw new UnsupportedOperationException("Just support MySQL by now!");
        }

        SQLRecognizer recognizer = resolveMySQLRecognizer(sql);
        return recognizer;
    }

    /**
     * resolve for mysql
     *
     * @param sql
     * @return
     */
    private static SQLRecognizer resolveMySQLRecognizer(String sql) {

        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        if (statements == null || statements.isEmpty()) {
            throw new UnsupportedOperationException("invalid sql:" + sql);
        }

        // pop hint statements
        List<String> sqlHints = new ArrayList<>();
        for (Integer i = 0; i < statements.size(); i++) {
            SQLStatement statement = statements.get(i);
            if (statement instanceof MySqlHintStatement) {
                MySqlHintStatement hintStatement = (MySqlHintStatement) statement;
                for (SQLCommentHint hint : hintStatement.getHints()) {
                    sqlHints.add(hint.getText());
                }
                statements.remove(statement);
                i--;
            }
        }

        if (statements.size() != 1) {
            throw new UnsupportedOperationException("invalid sql:" + sql);
        }

        SQLStatement ast = statements.get(0);
        SQLRecognizer recognizer = null;
        if (ast instanceof SQLInsertStatement) {
            recognizer = new MySQLInsertRecognizer(sql, ast, sqlHints);
        } else if (ast instanceof SQLUpdateStatement) {
            recognizer = new MySQLUpdateRecognizer(sql, ast, sqlHints);
        } else if (ast instanceof SQLDeleteStatement) {
            recognizer = new MySQLDeleteRecognizer(sql, ast, sqlHints);
        } else if (ast instanceof SQLSelectStatement) {
            if (((SQLSelectStatement) ast).getSelect().getQueryBlock().isForUpdate()) {
                recognizer = new MySQLSelectForUpdateRecognizer(sql, ast, sqlHints);
            }
        } else {
            throw new UnsupportedOperationException("invalid sql:" + sql);
        }
        return recognizer;
    }
}
