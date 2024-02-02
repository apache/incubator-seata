/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.druid.sqlserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLMergeStatement;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.util.JdbcConstants;


public abstract class BaseSqlServerRecognizer extends BaseRecognizer {
    /**
     * Instantiates a new sqlserver base recognizer.
     *
     * @param originalSql the original sql
     */
    public BaseSqlServerRecognizer(String originalSql) {
        super(originalSql);
    }

    public SQLServerOutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                                      final ArrayList<List<Object>> paramAppenderList,
                                                      final StringBuilder sb) {
        return new SQLServerOutputVisitor(sb) {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                //add a process of parameter extraction
                //parametersHolder——params of the same index; paramAppenderList——params of a sql statement
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues = parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        oneParamValues.forEach(param -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        //this is a one-time visit for building image
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }
                }
                return super.visit(x);
            }
        };
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

        executeVisit(where, new SQLServerOutputVisitor(sb));
        return sb.toString();
    }

    /**
     * method to deal top expression
     *
     * @param ast the statement
     */
    public void dealTop(SQLStatement ast) {
        throw new NotSupportYetException("Top expr is not supported");
    }

    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    @Override
    public boolean isSqlSyntaxSupports() {
        SQLServerASTVisitorAdapter visitor = new SQLServerASTVisitorAdapter() {
            @Override
            public boolean visit(SQLInSubQueryExpr x) {
                //just like: ...where id in (select id from t)
                throw new NotSupportYetException("not support the sql syntax with InSubQuery:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLSubqueryTableSource x) {
                //just like: select * from (select * from t)
                throw new NotSupportYetException("not support the sql syntax with SubQuery:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLReplaceStatement x) {
                //just like: replace into t (id,dr) values (1,'2'), (2,'3')
                throw new NotSupportYetException("not support the sql syntax with ReplaceStatement:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLMergeStatement x) {
                //just like: merge into ... WHEN MATCHED THEN ...
                throw new NotSupportYetException("not support the sql syntax with MergeStatement:" + x
                        + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLInsertStatement x) {
                if (null != x.getQuery()) {
                    //just like: insert into t select * from t1
                    throw new NotSupportYetException("not support the sql syntax insert with query:" + x
                            + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                }
                return true;
            }
        };
        getAst().accept(visitor);
        return true;
    }

}
