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

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLRecognizer;

/**
 * The type Base recognizer.
 *
 * @author sharajava
 */
public abstract class BaseRecognizer implements SQLRecognizer {

    /**
     * The type V marker.
     */
    public static class VMarker {
        @Override
        public String toString() {
            return "?";
        }

    }

    /**
     * The Original sql.
     */
    protected String originalSQL;

    /**
     * Instantiates a new Base recognizer.
     *
     * @param originalSQL the original sql
     */
    public BaseRecognizer(String originalSQL) {
        this.originalSQL = originalSQL;

    }

    public void executeVisit(SQLExpr where, SQLASTVisitor visitor) {
        if (where instanceof SQLBinaryOpExpr) {
            visitor.visit((SQLBinaryOpExpr) where);
        } else if (where instanceof SQLInListExpr) {
            visitor.visit((SQLInListExpr) where);
        } else if (where instanceof SQLBetweenExpr) {
            visitor.visit((SQLBetweenExpr) where);
        } else if (where instanceof SQLExistsExpr) {
            visitor.visit((SQLExistsExpr) where);
        } else if (where instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr whereMethod = (SQLMethodInvokeExpr) where;
            if (SupportSqlWhereMethod.getInstance().checkIsSupport(whereMethod.getMethodName())) {
                visitor.visit((SQLMethodInvokeExpr) where);
            } else {
                throw new IllegalArgumentException("not support where method: " + whereMethod.getMethodName());
            }
        } else {
            throw new IllegalArgumentException("unexpected WHERE expr: " + where.getClass().getSimpleName());
        }
    }

    protected void wrapSQLParsingException(SQLExpr expr) {
        String errorMsg;
        try {
            errorMsg =
                new StringBuilder("Unknown SQLExpr: ").append(expr.getClass()).append(" ").append(expr).toString();
        } catch (Exception e) {
            // druid 1.2.6 SQLObjectImpl#toString exist NPE https://github.com/alibaba/druid/issues/4290
            throw new SQLParsingException("Unknown SQLExpr: " + e.getMessage(), e);
        }
        throw new SQLParsingException(errorMsg);
    }

    public void executeLimit(SQLLimit sqlLimit, SQLASTVisitor visitor) {
        visitor.visit(sqlLimit);
    }

    public void executeOrderBy(SQLOrderBy sqlOrderBy,SQLASTVisitor visitor) {
        visitor.visit(sqlOrderBy);
    }

    @Override
    public String getOriginalSQL() {
        return originalSQL;
    }

    protected abstract SQLStatement getAst();

    @Override
    public boolean isSqlSyntaxSupports() {
        SQLASTVisitor visitor = new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLInSubQueryExpr x) {
                //just like: ...where id in (select id from t)
                throw new NotSupportYetException("not support the sql syntax with InSubQuery:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.io/zh-cn/docs/user/sqlreference/dml.html");
            }

            @Override
            public boolean visit(SQLSubqueryTableSource x) {
                //just like: select * from (select * from t)
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
}
