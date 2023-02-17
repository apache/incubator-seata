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
package io.seata.sqlparser.druid.sqlserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author GoodBoyCoder
 */
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
}
