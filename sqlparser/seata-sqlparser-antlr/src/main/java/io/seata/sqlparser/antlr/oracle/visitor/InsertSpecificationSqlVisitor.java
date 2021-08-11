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
package io.seata.sqlparser.antlr.oracle.visitor;

import io.seata.sqlparser.antlr.oracle.OracleContext;
import io.seata.sqlparser.antlr.oracle.parser.OracleBaseVisitor;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;

import java.util.Arrays;
import java.util.List;

/**
 * @author YechenGu
 */
public class InsertSpecificationSqlVisitor extends OracleBaseVisitor<OracleContext> {
    private OracleContext oracleContext;

    public InsertSpecificationSqlVisitor(OracleContext oracleContext) {
        this.oracleContext = oracleContext;
    }

    @Override
    public OracleContext visitSingle_table_insert(OracleParser.Single_table_insertContext ctx) {
        OracleParser.Insert_into_clauseContext insertIntoClauseContext = ctx.insert_into_clause();
        OracleParser.General_table_refContext generalTableRefContext = insertIntoClauseContext.general_table_ref();
        oracleContext.setTableName(generalTableRefContext.getText());

        OracleParser.Paren_column_listContext parenColumnListContext = insertIntoClauseContext.paren_column_list();
        if (parenColumnListContext != null){
            OracleParser.Column_listContext columnListContext = parenColumnListContext.column_list();
            List<OracleParser.Column_nameContext> columnNameContexts = columnListContext.column_name();
            for (OracleParser.Column_nameContext columnNameContext:columnNameContexts){
                oracleContext.addForInsertColumnName(columnNameContext.getText());
            }
        }

        OracleParser.Values_clauseContext valuesClauseContext = ctx.values_clause();
        OracleParser.ExpressionsContext expressionsContext = valuesClauseContext.expressions();
        if (expressionsContext != null){
            List<OracleParser.ExpressionContext> expressionContexts = expressionsContext.expression();
            oracleContext.setInsertRows(expressionContexts.size());
            for (OracleParser.ExpressionContext expressionContext:expressionContexts){
                oracleContext.addForInsertValColumnName(expressionContext.getText());
            }
        }

        return this.oracleContext;
    }
}
