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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLBlockStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * DruidSQLRecognizerFactoryImpl
 *
 * @author sharajava
 * @author ggndnn
 */
class DruidSQLRecognizerFactoryImpl implements SQLRecognizerFactory {
    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, dbType);
        if (CollectionUtils.isEmpty(asts)) {
            throw new UnsupportedOperationException("Unsupported SQL: " + sql);
        }
        if (asts.size() > 1 && !(asts.stream().allMatch(statement -> statement instanceof SQLUpdateStatement)
                || asts.stream().allMatch(statement -> statement instanceof SQLDeleteStatement))) {
            throw new UnsupportedOperationException("ONLY SUPPORT SAME TYPE (UPDATE OR DELETE) MULTI SQL -" + sql);
        }
        List<SQLRecognizer> recognizers = new ArrayList<>();
        SQLRecognizer recognizer = null;
        for (SQLStatement ast : asts) {
            SQLOperateRecognizerHolder recognizerHolder =
                    SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if (ast instanceof SQLInsertStatement ) {
                recognizer = recognizerHolder.getInsertRecognizer(sql, ast);
            }
            // begin insert table values();insert table values()  end;
            else if( ast instanceof  SQLBlockStatement && ((SQLBlockStatement) ast).getStatementList().get(0)  instanceof SQLInsertStatement) {
                SQLBlockStatement sqlBlockStatement = (SQLBlockStatement)ast;
                List<SQLStatement> list = sqlBlockStatement.getStatementList();
                for(SQLStatement statement:list) {
                    recognizers.add(recognizerHolder.getInsertRecognizer(statement.toLowerCaseString(), statement));
                }
                break;
            }
            // insert all into table  values() select * from dual
            else if(ast instanceof OracleMultiInsertStatement) {
                recognizer = recognizerHolder.getMultiInsertStatement(sql, ast);
            } else if (ast instanceof SQLUpdateStatement ) {
                recognizer = recognizerHolder.getUpdateRecognizer(sql, ast);
            }
            // // begin update table set a = b;update table set a= c where  end;
            else if( ast instanceof  SQLBlockStatement && ((SQLBlockStatement) ast).getStatementList().get(0)  instanceof SQLUpdateStatement) {
                SQLBlockStatement sqlBlockStatement = (SQLBlockStatement)ast;
                List<SQLStatement> list = sqlBlockStatement.getStatementList();
                for(SQLStatement statement:list) {
                    recognizers.add(recognizerHolder.getUpdateRecognizer(statement.toLowerCaseString(), statement));
                }
                break;
            } else if (ast instanceof SQLDeleteStatement ) {
                recognizer = recognizerHolder.getDeleteRecognizer(sql, ast);
            }
            // // begin delete table where;delete table where  end;
            else if( ast instanceof  SQLBlockStatement && ((SQLBlockStatement) ast).getStatementList().get(0)  instanceof SQLDeleteStatement) {
                SQLBlockStatement sqlBlockStatement = (SQLBlockStatement)ast;
                List<SQLStatement> list = sqlBlockStatement.getStatementList();
                for(SQLStatement statement:list) {
                    recognizers.add(recognizerHolder.getDeleteRecognizer(statement.toLowerCaseString(), statement));
                }
                break;
            }  else if (ast instanceof SQLSelectStatement) {
                recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql, ast);
            }
            if (recognizer != null) {
                if (recognizers == null) {
                    recognizers = new ArrayList<>();
                }
                recognizers.add(recognizer);
            }
        }
        return recognizers;
    }

    private boolean isOraceBeginEndUpdateSql(String dbType,SQLStatement ast) {
        if("oracle".equals(dbType.toLowerCase()) && ast instanceof SQLBlockStatement && ((SQLBlockStatement) ast).getStatementList().get(0) instanceof SQLUpdateStatement){
            return  true;
        } else {
            return  false;
        }
    }
    private boolean isOraceBeginEndDeleteSql(String dbType,SQLStatement ast) {
        if("oracle".equals(dbType.toLowerCase()) && ast instanceof SQLBlockStatement && ((SQLBlockStatement) ast).getStatementList().get(0) instanceof SQLUpdateStatement){
            return  true;
        } else {
            return  false;
        }
    }
}
