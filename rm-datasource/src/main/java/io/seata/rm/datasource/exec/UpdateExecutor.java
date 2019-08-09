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

import io.seata.rm.datasource.ParametersHolder;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.SQLUpdateRecognizer;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;

import org.apache.commons.lang.StringUtils;

/**
 * The type Update executor.
 *
 * @author sharajava
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class UpdateExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    /**
     * Instantiates a new Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public UpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                          SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {

        ArrayList<List<Object>> paramAppenders = new ArrayList<>();
        TableMeta tmeta = getTableMeta();
        String selectSQL = buildBeforeImageSQL(tmeta, paramAppenders);
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

    private String buildBeforeImageSQL(TableMeta tableMeta, ArrayList<List<Object>> paramAppenders) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer)sqlRecognizer;
        List<String> updateColumns = recognizer.getUpdateColumns();
        StringBuffer selectSQLPrefix = new StringBuffer("SELECT ");
        if (!tableMeta.containsPK(updateColumns)) {
            selectSQLPrefix.append(getColumnNameInSQL(tableMeta.getPkName()) + ", ");
        }
        String prefix = selectSQLPrefix.toString();
        String whereCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = recognizer.getWhereCondition((ParametersHolder)statementProxy, paramAppenders);
        } else {
            whereCondition = recognizer.getWhereCondition();
        }
        StringBuffer selectSQLSuffix = new StringBuffer(" FROM " + getFromTableInSQL());
        if (StringUtils.isNotBlank(whereCondition)) {
            selectSQLSuffix.append(" WHERE " + whereCondition);
        }
        selectSQLSuffix.append(" FOR UPDATE");
        String suffix = selectSQLSuffix.toString();
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix, suffix);
        for (String updateColumn : updateColumns) {
            selectSQLJoin.add(updateColumn);
        }
        String selectSQL = selectSQLJoin.toString();
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
        TableMeta tmeta = getTableMeta();
        if (beforeImage == null || beforeImage.size() == 0) {
            return TableRecords.empty(getTableMeta());
        }
        String selectSQL = buildAfterImageSQL(tmeta, beforeImage);
        TableRecords afterImage = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = statementProxy.getConnection().prepareStatement(selectSQL);
            int index = 0;
            for (Field pkField : beforeImage.pkRows()) {
                index++;
                pst.setObject(index, pkField.getValue(), pkField.getType());
            }
            rs = pst.executeQuery();
            afterImage = TableRecords.buildRecords(tmeta, rs);

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pst != null) {
                pst.close();
            }
        }
        return afterImage;
    }

    private String buildAfterImageSQL(TableMeta tableMeta, TableRecords beforeImage) throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer)sqlRecognizer;
        List<String> updateColumns = recognizer.getUpdateColumns();
        StringBuffer selectSQLPrefix = new StringBuffer("SELECT ");
        if (!tableMeta.containsPK(updateColumns)) {
            // PK should be included.
            selectSQLPrefix.append(getColumnNameInSQL(tableMeta.getPkName()) + ", ");
        }
        String prefix = selectSQLPrefix.toString();
        String suffix = " FROM " + getFromTableInSQL() + " WHERE " + buildWhereConditionByPKs(beforeImage.pkRows());
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix, suffix);
        for (String column : updateColumns) {
            selectSQLJoiner.add(column);
        }
        return selectSQLJoiner.toString();
    }
}
