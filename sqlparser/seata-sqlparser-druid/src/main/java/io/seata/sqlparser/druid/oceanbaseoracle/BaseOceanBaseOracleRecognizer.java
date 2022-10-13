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
package io.seata.sqlparser.druid.oceanbaseoracle;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.sqlparser.druid.oracle.BaseOracleRecognizer;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * Base sql recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public abstract class BaseOceanBaseOracleRecognizer extends BaseOracleRecognizer {

    public BaseOceanBaseOracleRecognizer(String originalSql) {
        super(originalSql);
    }

    @Override
    public String getTableAlias() {
        SQLTableSource tableSource = getTableSource();
        if (tableSource == null) {
            throw new ShouldNeverHappenException("Unable to recognize table source: " + getOriginalSQL());
        }
        return tableSource.getAlias();
    }

    @Override
    public String getTableName() {
        SQLTableSource tableSource = getTableSource();
        if (tableSource == null) {
            throw new ShouldNeverHappenException("Unable to recognize table source: " + getOriginalSQL());
        }
        StringBuilder tableName = new StringBuilder();
        if (tableSource instanceof SQLExprTableSource) {
            OracleOutputVisitor visitor = new OracleOutputVisitor(tableName) {
                @Override
                public boolean visit(SQLExprTableSource x) {
                    printTableSourceExpr(x.getExpr());
                    return false;
                }
            };
            visitor.visit((SQLExprTableSource) tableSource);
        } else {
            throw new NotSupportYetException("Not supported syntax with table source: " +
                tableSource.getClass().getName());
        }
        return tableName.toString();
    }

    public String getDbType() {
        return JdbcConstants.OCEANBASE_ORACLE;
    }

    protected abstract SQLTableSource getTableSource();
}
