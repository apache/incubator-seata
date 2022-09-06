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
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.oceanbaseoracle.OceanBaseOracleOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * DruidSQLRecognizerFactoryImpl
 *
 * @author sharajava
 * @author ggndnn
 */
class DruidSQLRecognizerFactoryImpl implements SQLRecognizerFactory {
    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (CollectionUtils.isEmpty(asts)) {
            throw new UnsupportedOperationException("Not supported SQL: " + sql);
        }
        List<SQLRecognizer> recognizers = new ArrayList<>();
        for (SQLStatement ast : asts) {
            SQLOperateRecognizerHolder recognizerHolder =
                SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if (ast instanceof SQLInsertStatement) {
                recognizers.add(recognizerHolder.getInsertRecognizer(sql, ast));
            } else if (ast instanceof SQLUpdateStatement) {
                recognizers.add(recognizerHolder.getUpdateRecognizer(sql, ast));
            } else if (ast instanceof SQLDeleteStatement) {
                recognizers.add(recognizerHolder.getDeleteRecognizer(sql, ast));
            } else if (ast instanceof SQLSelectStatement) {
                recognizers.add(recognizerHolder.getSelectForUpdateRecognizer(sql, ast));
            } else if (ast instanceof OracleMultiInsertStatement) {
                if (recognizerHolder instanceof OceanBaseOracleOperateRecognizerHolder) {
                    recognizers.addAll(((OceanBaseOracleOperateRecognizerHolder) recognizerHolder)
                        .getMultiInsertStatement(sql, ast));
                }
            }
        }
        // check if recognizers are supported
        if (!recognizers.stream().allMatch(this::isSupportedRecognizer)) {
            throw new NotSupportYetException("Not supported SQL: " + sql);
        }
        // check if multi recognizers are supported
        if (recognizers.size() > 1 && !(recognizers.stream().allMatch(r -> SQLType.UPDATE.equals(r.getSQLType()))
            || recognizers.stream().allMatch(r -> SQLType.DELETE.equals(r.getSQLType()))
            || dbType.equals(JdbcConstants.OCEANBASE_ORACLE)
            && recognizers.stream().allMatch(r -> SQLType.INSERT.equals(r.getSQLType())))
        ) {
            throw new NotSupportYetException(
                "Only multiple sql of the same type (insert, update or delete) are supported: " + sql);
        }
        return recognizers;
    }

    private boolean isSupportedRecognizer(SQLRecognizer sqlRecognizer) {
        return sqlRecognizer != null && sqlRecognizer.isSqlSyntaxSupports();
    }
}
