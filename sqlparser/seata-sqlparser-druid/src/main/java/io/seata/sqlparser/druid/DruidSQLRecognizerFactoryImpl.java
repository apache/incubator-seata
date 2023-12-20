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
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.druid.oceanbaseoracle.OceanBaseOracleOperateRecognizerHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DruidSQLRecognizerFactoryImpl
 *
 * @author sharajava
 * @author ggndnn
 * @author hsien999
 */
class DruidSQLRecognizerFactoryImpl implements SQLRecognizerFactory {
    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DruidDbTypeAdapter.getAdaptiveDbType(dbType));
        if (CollectionUtils.isEmpty(asts)) {
            throw new UnsupportedOperationException("Not supported SQL: " + sql);
        }
        if (asts.size() > 1 && !(asts.stream().allMatch(statement -> statement instanceof SQLUpdateStatement)
            || asts.stream().allMatch(statement -> statement instanceof SQLDeleteStatement)
            || asts.stream().allMatch(statement -> statement instanceof OracleMultiInsertStatement))) {
            throw new UnsupportedOperationException(
                "Only multiple sql of the same type (UPDATE, DELETE, or INSERT in Oracle) are supported: " + sql);
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
        // filter recognizers that are not supported
        recognizers = recognizers.stream().filter(this::isSupportedRecognizer).collect(Collectors.toList());
        return recognizers.isEmpty() ? null : recognizers;
    }

    private boolean isSupportedRecognizer(SQLRecognizer sqlRecognizer) {
        return sqlRecognizer != null && sqlRecognizer.isSqlSyntaxSupports();
    }
}
