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

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectJoin;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectSubqueryTableSource;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitor;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base sql recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public abstract class BaseOceanBaseOracleRecognizer extends BaseRecognizer {

    public BaseOceanBaseOracleRecognizer(String originalSql) {
        super(originalSql);
    }

    protected String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }
        StringBuilder whereStr = new StringBuilder();
        executeVisit(where, new OracleOutputVisitor(whereStr));
        return whereStr.toString();
    }

    protected String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder,
                                       final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }
        StringBuilder whereStr = new StringBuilder();
        executeVisit(where, createParameterVisitor(parametersHolder, paramAppenderList, whereStr));
        return whereStr.toString();
    }

    protected String getOrderByCondition(SQLOrderBy orderBy) {
        if (Objects.isNull(orderBy)) {
            return StringUtils.EMPTY;
        }
        StringBuilder orderByStr = new StringBuilder();
        executeOrderBy(orderBy, new OracleOutputVisitor(orderByStr));
        return orderByStr.toString();
    }

    protected String getOrderByCondition(SQLOrderBy orderBy, final ParametersHolder parametersHolder,
                                         final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(orderBy)) {
            return StringUtils.EMPTY;
        }
        StringBuilder orderByStr = new StringBuilder();
        executeOrderBy(orderBy, createParameterVisitor(parametersHolder, paramAppenderList, orderByStr));
        return orderByStr.toString();
    }

    protected OracleOutputVisitor createParameterVisitor(final ParametersHolder parametersHolder,
                                                         final ArrayList<List<Object>> paramAppenderList,
                                                         final StringBuilder sb) {
        // visit variant reference to construct parameter object list
        return new OracleOutputVisitor(sb) {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        // batch operations assume that the list of values for each parameter index has the same size
                        oneParamValues.forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }
                }
                return super.visit(x);
            }
        };
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

    @Override
    public boolean isSqlSyntaxSupports() {
        String prefix = "Not supported sql syntax with ";
        String suffix = "\nPlease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html";
        OracleASTVisitor visitor = new OracleASTVisitorAdapter() {
            @Override
            public boolean visit(OracleSelectJoin x) {
                // e.g. SELECT * FROM a INNER JOIN b ON a.id = b.id ...
                throw new NotSupportYetException(prefix + "'select joined table':" + x + suffix);
            }

            @Override
            public boolean visit(OracleSelectSubqueryTableSource x) {
                // e.g. SELECT * FROM (SELECT * FROM a) ...
                throw new NotSupportYetException(prefix + "'select sub query':" + x + suffix);
            }

            @Override
            public boolean visit(SQLJoinTableSource x) {
                // e.g. ... FROM a INNER JOIN b ON a.id = b.id ...
                throw new NotSupportYetException(prefix + "'joined table source':" + x + suffix);
            }

            @Override
            public boolean visit(SQLInSubQueryExpr x) {
                // e.g. ... WHERE id IN (SELECT id FROM a) ...
                throw new NotSupportYetException(prefix + "'in sub query':" + x + suffix);
            }

            @Override
            public boolean visit(SQLReplaceStatement x) {
                // e.g. REPLACE INTO a(id) VALUES (1) ...
                throw new NotSupportYetException(prefix + "'replace':" + x + suffix);
            }

            @Override
            public boolean visit(SQLMergeStatement x) {
                // e.g. MERGE INTO a USING b ON ... WHEN MATCHED THEN UPDATE SET ... WHEN NOT MATCHED THEN INSERT ...
                throw new NotSupportYetException(prefix + "'merge':" + x + suffix);
            }

            @Override
            public boolean visit(SQLInsertStatement x) {
                if (null != x.getQuery()) {
                    // e.g. INSERT INTO a SELECT * FROM b
                    throw new NotSupportYetException(prefix + "'insert into sub query':" + x + suffix);
                }
                return true;
            }
        };
        getAst().accept(visitor);
        return true;
    }

    protected abstract SQLTableSource getTableSource();
}
