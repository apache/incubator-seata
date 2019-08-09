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
package io.seata.rm.datasource.exec;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.ParametersHolder;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLDeleteRecognizer;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;

import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import org.apache.commons.lang.StringUtils;

/**
 * The type Delete executor.
 *
 * @author sharajava
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class DeleteExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    /**
     * Instantiates a new Delete executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public DeleteExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                          SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        SQLDeleteRecognizer visitor = (SQLDeleteRecognizer) sqlRecognizer;
        TableMeta tmeta = getTableMeta(visitor.getTableName());
        ArrayList<List<Object>> paramAppenders = new ArrayList<>();
        String selectSQL = buildBeforeImageSQL(visitor, tmeta, paramAppenders);
        TableRecords beforeImage = null;
        PreparedStatement ps = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            if (paramAppenders.isEmpty()) {
                st = statementProxy.getConnection().createStatement();
                rs = st.executeQuery(selectSQL);
            } else {
                if (paramAppenders.size() == 1) {
                    ps = statementProxy.getConnection().prepareStatement(selectSQL);
                    List<Object> paramAppender = paramAppenders.get(0);
                    for (int i = 0; i < paramAppender.size(); i++) {
                        ps.setObject(i + 1, paramAppender.get(i));
                    }
                } else {
                    ps = statementProxy.getConnection().prepareStatement(selectSQL);
                    List<Object> paramAppender = null;
                    for (int i = 0; i < paramAppenders.size(); i++) {
                        paramAppender = paramAppenders.get(i);
                        for (int j = 0; j < paramAppender.size(); j++) {
                            ps.setObject(i * paramAppender.size() + j + 1, paramAppender.get(j));
                        }
                    }
                }
                rs = ps.executeQuery();
            }
            beforeImage = TableRecords.buildRecords(tmeta, rs);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return beforeImage;
    }

    private String buildBeforeImageSQL(SQLDeleteRecognizer visitor, TableMeta tableMeta, ArrayList<List<Object>> paramAppenders) {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        String whereCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = visitor.getWhereCondition((ParametersHolder) statementProxy, paramAppenders);
        } else {
            whereCondition = visitor.getWhereCondition();
        }
        StringBuffer sqlSuffix = new StringBuffer(" FROM " + keywordChecker.checkAndReplace(getFromTableInSQL()));
        if (StringUtils.isNotBlank(whereCondition)) {
            sqlSuffix.append(" WHERE " + whereCondition);
        }
        sqlSuffix.append(" FOR UPDATE");
        String suffix = sqlSuffix.toString();
        StringJoiner selectSQLAppender = new StringJoiner(", ", "SELECT ", suffix);
        for (String column : tableMeta.getAllColumns().keySet()) {
            selectSQLAppender.add(getColumnNameInSQL(keywordChecker.checkAndReplace(column)));
        }
        String selectSQL = selectSQLAppender.toString();
        if(!paramAppenders.isEmpty() && paramAppenders.size() > 1) {
            StringBuffer stringBuffer = new StringBuffer(selectSQL);
            for (int i = 1; i < paramAppenders.size(); i++) {
                stringBuffer.append(" UNION ").append(selectSQL);
            }
            selectSQL = stringBuffer.toString();
        }
        return selectSQL;
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return TableRecords.empty(getTableMeta());
    }
}
