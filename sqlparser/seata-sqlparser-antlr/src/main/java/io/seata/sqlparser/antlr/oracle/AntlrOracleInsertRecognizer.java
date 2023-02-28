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

import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.oracle.visitor.InsertStatementSqlVisitor;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author YechenGu
 */
public class AntlrOracleInsertRecognizer implements SQLInsertRecognizer {
    private OracleContext sqlContext;

    public AntlrOracleInsertRecognizer(String sql) {
        OracleLexer lexer = new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser = new OracleParser(tokenStream);
        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        sqlContext = new OracleContext();
        sqlContext.setOriginalSQL(sql);
        InsertStatementSqlVisitor visitor = new InsertStatementSqlVisitor(sqlContext);
        visitor.visit(scriptContext);
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        return sqlContext.tableAlias;
    }

    @Override
    public String getTableName() {
        return sqlContext.tableName;
    }

    @Override
    public String getOriginalSQL() {
        return sqlContext.getOriginalSQL();
    }

    @Override
    public boolean insertColumnsIsEmpty() {

        List<OracleContext.SQL> insertColumnNames = sqlContext.getInsertColumnNames();

        if (insertColumnNames.isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public List<String> getInsertColumns() {

        List<OracleContext.SQL> insertColumnNames = sqlContext.getInsertColumnNames();

        if (insertColumnNames.isEmpty()) {
            return new ArrayList<>();
        }

        return insertColumnNames.stream().map(insertColumns -> insertColumns.getColumnName()).collect(Collectors.toList());
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        return null;
    }

    @Override
    public List<String> getInsertParamsValue() {
        return null;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }

    @Override
    public List<String> getInsertColumnsIsSimplified() {
        List<String> insertColumns = getInsertColumns();
        return ColumnUtils.delEscape(insertColumns, JdbcConstants.ORACLE);
    }
}
