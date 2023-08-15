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
package io.seata.sqlparser.druid.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectJoin;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectSubqueryTableSource;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitor;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author will
 */
public abstract class BaseOracleRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new oracle base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseOracleRecognizer(String originalSql) {
        super(originalSql);
    }

    public OracleOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                                   final ArrayList<List<Object>> paramAppenderList,
                                                   final StringBuilder sb) {

        return new OracleOutputVisitor(sb) {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
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

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeVisit(where, new OracleOutputVisitor(sb));
        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, new OracleOutputVisitor(sb));

        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy, final ParametersHolder parametersHolder,
                                         final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    @Override
    public boolean isSqlSyntaxSupports() {
        OracleASTVisitor visitor = new OracleASTVisitorAdapter() {
            @Override
            public boolean visit(OracleSelectJoin x) {
                //just like: UPDATE table a INNER JOIN table b ON a.id = b.pid ...
                throw new NotSupportYetException("not support the sql syntax with join table:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(OracleUpdateStatement x) {
                if (x.getTableSource() instanceof OracleSelectSubqueryTableSource) {
                    //just like: "update (select a.id,a.name from a inner join b on a.id = b.id) t set t.name = 'xxx'"
                    throw new NotSupportYetException("not support the sql syntax with join table:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
                }
                List<SQLUpdateSetItem> updateSetItems = x.getItems();
                for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                    if (updateSetItem.getValue() instanceof SQLQueryExpr) {
                        //just like: "update a set a.id = (select id from b where a.pid = b.pid)"
                        throw new NotSupportYetException("not support the sql syntax with join table:" + x
                            + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
                    }
                }
                return true;
            }

            @Override
            public boolean visit(SQLInSubQueryExpr x) {
                //just like: ...where id in (select id from t)
                throw new NotSupportYetException("not support the sql syntax with InSubQuery:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(OracleSelectSubqueryTableSource x) {
                //just like: select * from (select * from t) for update
                throw new NotSupportYetException("not support the sql syntax with SubQuery:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(SQLReplaceStatement x) {
                //just like: replace into t (id,dr) values (1,'2'), (2,'3')
                throw new NotSupportYetException("not support the sql syntax with ReplaceStatement:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(SQLMergeStatement x) {
                //just like: merge into ... WHEN MATCHED THEN ...
                throw new NotSupportYetException("not support the sql syntax with MergeStatement:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(SQLInsertStatement x) {
                if (null != x.getQuery()) {
                    //just like: insert into t select * from t1
                    throw new NotSupportYetException("not support the sql syntax insert with query:" + x
                            + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
                }
                return true;
            }
        };
        getAst().accept(visitor);
        return true;
    }

    public String getDbType() {
        return JdbcConstants.ORACLE;
    }
}
