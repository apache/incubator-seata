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
package io.seata.sqlparser.antlr.oracle;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.antlr.oracle.listener.UpdateSpecificationSqlListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YechenGu
 */
public class AntlrOracleUpdateRecognizer implements SQLUpdateRecognizer {
    private OracleContext oracleContext;

    public AntlrOracleUpdateRecognizer(String sql) {
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        context.setOriginalSQL(sql);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(context),scriptContext);
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public String getTableAlias() {
        return oracleContext.tableAlias;
    }

    @Override
    public String getTableName() {
        return oracleContext.tableName;
    }

    @Override
    public String getOriginalSQL() {
        return oracleContext.getOriginalSQL();
    }

    @Override
    public List<String> getUpdateColumns() {
        List<OracleContext.SQL> updateColumnNames = oracleContext.getUpdateColumnNames();
        ArrayList<String> list = new ArrayList<>();
        for (OracleContext.SQL sql : updateColumnNames){
            list.add(sql.getUpdateColumn());
        }
        return list;
    }

    @Override
    public List<Object> getUpdateValues() {
        List<OracleContext.SQL> updateColumnValues = oracleContext.getUpdateColumnValues();

        if (updateColumnValues.isEmpty()){
            return new ArrayList<>();
        }

        return updateColumnValues.stream().map(updateValues -> updateValues.getUpdateValue()).collect(Collectors.toList());
    }

    @Override
    public List<String> getUpdateColumnsIsSimplified() {
        List<String> updateColumns = getUpdateColumns();
        return ColumnUtils.delEscape(updateColumns, JdbcConstants.ORACLE);
    }


    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return oracleContext.getWhereCondition();
    }

    @Override
    public String getWhereCondition() {
        return oracleContext.getWhereCondition();
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
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }
}
