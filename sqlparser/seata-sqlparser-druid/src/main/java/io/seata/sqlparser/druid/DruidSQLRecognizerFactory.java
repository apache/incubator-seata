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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SqlParserType;

import java.util.List;

/**
 * DruidSQLRecognizerFactory
 *
 * @author sharajava
 * @author ggndnn
 */
@LoadLevel(name = SqlParserType.SQL_PARSER_TYPE_DRUID)
public class DruidSQLRecognizerFactory implements SQLRecognizerFactory {
    @Override
    public SQLRecognizer create(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (asts == null || asts.size() != 1) {
            throw new UnsupportedOperationException("Unsupported SQL: " + sql);
        }
        SQLRecognizer recognizer = null;
        SQLStatement ast = asts.get(0);
        SQLOperateRecognizerHolder recognizerHolder =
                SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
        if (ast instanceof SQLInsertStatement) {
            recognizer = recognizerHolder.getInsertRecognizer(sql, ast);
        } else if (ast instanceof SQLUpdateStatement) {
            recognizer = recognizerHolder.getUpdateRecognizer(sql, ast);
        } else if (ast instanceof SQLDeleteStatement) {
            recognizer = recognizerHolder.getDeleteRecognizer(sql, ast);
        } else if (ast instanceof SQLSelectStatement) {
            recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql, ast);
        }
        return recognizer;
    }
}
