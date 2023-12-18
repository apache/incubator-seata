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

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectForUpdate recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleSelectForUpdateRecognizer extends BaseOceanBaseOracleRecognizer implements SQLSelectRecognizer {
    private final SQLSelectStatement ast;

    public OceanBaseOracleSelectForUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLSelectStatement) ast;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.SELECT_FOR_UPDATE;
    }

    @Override
    protected SQLTableSource getTableSource() {
        return getSelect().getFrom();
    }

    @Override
    public String getWhereCondition() {
        return super.getWhereCondition(getSelect().getWhere());
    }

    @Override
    public String getWhereCondition(final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        return super.getWhereCondition(getSelect().getWhere(), parametersHolder, paramAppenderList);
    }

    @Override
    public String getLimitCondition() {
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public String getOrderByCondition() {
        return super.getOrderByCondition(getSelect().getOrderBy());
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return super.getOrderByCondition(getSelect().getOrderBy(), parametersHolder, paramAppenderList);
    }

    private SQLSelectQueryBlock getSelect() {
        SQLSelect select = ast.getSelect();
        if (select == null) {
            throw new SQLParsingException("Empty select statement: " + getOriginalSQL());
        }
        SQLSelectQueryBlock selectQueryBlock = select.getQueryBlock();
        if (selectQueryBlock == null) {
            throw new SQLParsingException("Empty select query block: " + getOriginalSQL());
        }
        return selectQueryBlock;
    }
}
