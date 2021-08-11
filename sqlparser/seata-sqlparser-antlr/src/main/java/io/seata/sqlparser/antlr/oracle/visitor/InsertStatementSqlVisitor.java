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

/**
 * @author YechenGu
 */
public class InsertStatementSqlVisitor extends OracleBaseVisitor<OracleContext> {
    private OracleContext oracleContext;

    public InsertStatementSqlVisitor(OracleContext oracleContext) {
        this.oracleContext = oracleContext;
    }

    @Override
    public OracleContext visitSingle_table_insert(OracleParser.Single_table_insertContext ctx) {
        return new InsertSpecificationSqlVisitor(this.oracleContext).visitSingle_table_insert(ctx);
    }
}
