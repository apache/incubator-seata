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
package org.apache.seata.sqlparser.druid.oceanbase;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseOceanBaseRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new OceanBase base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseOceanBaseRecognizer(String originalSql) {
        super(originalSql);
    }

    public SQLASTOutputVisitor createOracleOutputVisitor(
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList,
            final StringBuilder sb) {

        return new OracleOutputVisitor(sb) {
            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues =
                            parametersHolder.getParameters().get(x.getIndex() + 1);
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

    public String getWhereCondition(
            SQLExpr where, final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeVisit(where, createOracleOutputVisitor(parametersHolder, paramAppenderList, sb));
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

    protected String getOrderByCondition(
            SQLOrderBy sqlOrderBy,
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, createOracleOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getDbType() {
        return JdbcConstants.OCEANBASE;
    }
}
