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
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.antlr.oracle.listener.DeleteSpecificationSqlListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YechenGu
 */
public class AntlrOracleDeleteRecognizer implements SQLSelectRecognizer {
    private OracleContext oracleContext;

    public AntlrOracleDeleteRecognizer(String sql) {
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        context.setOriginalSQL(sql);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(context),scriptContext);
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.DELETE;
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
