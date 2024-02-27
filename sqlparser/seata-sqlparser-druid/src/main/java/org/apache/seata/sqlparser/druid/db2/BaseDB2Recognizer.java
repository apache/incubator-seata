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
package org.apache.seata.sqlparser.druid.db2;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.dialect.db2.visitor.DB2OutputVisitor;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.util.JdbcConstants;


/**
 * @author qingjiusanliangsan
 */
public abstract class BaseDB2Recognizer extends BaseRecognizer {

    /**
     * Instantiates a new db2 base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseDB2Recognizer(String originalSql) {
        super(originalSql);
    }

    public DB2OutputVisitor createOutputVisitor(final ParametersHolder parametersHolder,
                                                final ArrayList<List<Object>> paramAppenderList, final StringBuilder sb) {
        DB2OutputVisitor visitor = new DB2OutputVisitor(sb) {

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
        executeVisit(where, new DB2OutputVisitor(sb));
        return sb.toString();
    }

    public String getDbType() {
        return JdbcConstants.DB2;
    }

}
