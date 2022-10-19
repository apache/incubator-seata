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

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGInsertStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGUpdateStatement;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGASTVisitor;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author will
 */
public abstract class BasePostgresqlRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new postgresql base recognizer
     *
     * @param originalSql the original sql
     */
    public BasePostgresqlRecognizer(String originalSql) {
        super(originalSql);
    }

    public PGOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
        final ArrayList<List<Object>> paramAppenderList, final StringBuilder sb) {
        PGOutputVisitor visitor = new PGOutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.size() == 0) {
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
        return visitor;
    }

    @Override
    public boolean isSqlSyntaxSupports() {
        PGASTVisitor visitor = new PGASTVisitorAdapter() {

            @Override
            public boolean visit(SQLSubqueryTableSource x) {
                //just like: select * from (select * from t) for update
                throw new NotSupportYetException("not support the sql syntax with SubQuery:" + x
                    + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(PGUpdateStatement x) {
                if (x.getFrom() != null) {
                    //just like: update a set id = b.pid from b where a.id = b.id
                    throw new NotSupportYetException("not support the sql syntax with join table:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
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
            public boolean visit(PGInsertStatement x) {
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

    public String getWhereCondition(SQLExpr where, final ParametersHolder parametersHolder,
        final ArrayList<List<Object>> paramAppenderList) {
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
        executeVisit(where, new PGOutputVisitor(sb));
        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeLimit(sqlLimit, new PGOutputVisitor(sb));

        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit, final ParametersHolder parametersHolder,
                                       final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeLimit(sqlLimit, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, new PGOutputVisitor(sb));

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

    public String getDbType() {
        return JdbcConstants.POSTGRESQL;
    }
}
