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
package io.seata.sqlparser.antlr.mysql.visit;

import io.seata.sqlparser.antlr.mysql.parser.MySqlParserBaseVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * StatementSqlVisitor
 *
 * @author zhihou
 */
public class StatementSqlVisitor extends MySqlParserBaseVisitor<StringBuilder> {

    private StringBuilder sb = new StringBuilder();

    @Override
    public StringBuilder visitTerminal(TerminalNode node) {
        String text = node.getText();
        if (text != null && !"".equals(text.trim())) {
            if (shouldAddSpace(text.trim())) {
                sb.append(" ");
            }
            sb.append(text);
        }
        return sb;
    }

    private boolean shouldAddSpace(String text) {
        if (sb.length() == 0) {
            return false;
        }
        char lastChar = sb.charAt(sb.length() - 1);
        switch (lastChar) {
            case '.':
            case ',':
            case '(':
                return false;
            default:
                break;
        }

        switch (text.charAt(0)) {
            case '.':
            case ',':
            case ')':
                return false;
            default:
                break;
        }
        return true;
    }


}