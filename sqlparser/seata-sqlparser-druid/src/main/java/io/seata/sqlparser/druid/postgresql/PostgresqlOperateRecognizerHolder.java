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
package io.seata.sqlparser.druid.postgresql;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectQueryBlock;
import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type PostgresqlOperateRecognizerHolder
 *
 * @author will.zjw
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL)
public class PostgresqlOperateRecognizerHolder implements SQLOperateRecognizerHolder {

    @Override
    public SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast) {
        PostgresqlDeleteRecognizer postgresqlDeleteRecognizer = new PostgresqlDeleteRecognizer(sql, ast);
        if (postgresqlDeleteRecognizer.isSqlSyntaxSupports(ast)) {
            return postgresqlDeleteRecognizer;
        }
        return null;
    }

    @Override
    public SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast) {
        PostgresqlInsertRecognizer postgresqlInsertRecognizer = new PostgresqlInsertRecognizer(sql, ast);
        if (postgresqlInsertRecognizer.isSqlSyntaxSupports(ast)) {
            return postgresqlInsertRecognizer;
        }
        return null;
    }

    @Override
    public SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast) {
        PostgresqlUpdateRecognizer postgresqlUpdateRecognizer = new PostgresqlUpdateRecognizer(sql, ast);
        if (postgresqlUpdateRecognizer.isSqlSyntaxSupports(ast)) {
            return postgresqlUpdateRecognizer;
        }
        return null;
    }

    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast) {
        PGSelectQueryBlock selectQueryBlock = (PGSelectQueryBlock) ((SQLSelectStatement) ast).getSelect().getFirstQueryBlock();
        if (selectQueryBlock.getForClause() != null && selectQueryBlock.getForClause().getOption().equals(PGSelectQueryBlock.ForClause.Option.UPDATE)) {
            PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, ast);
            if (postgresqlSelectForUpdateRecognizer.isSqlSyntaxSupports(ast)) {
                return postgresqlSelectForUpdateRecognizer;
            }
        }
        return null;
    }
}
