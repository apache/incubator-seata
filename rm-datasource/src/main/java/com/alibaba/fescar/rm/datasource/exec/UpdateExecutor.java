/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.exec;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fescar.rm.datasource.ParametersHolder;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLUpdateRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

/**
 * The type Update executor.
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
    public UpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer)sqlRecognizer;

        TableMeta tmeta = getTableMeta();
        List<String> updateColumns = recognizer.getUpdateColumns();

        StringBuffer selectSQLAppender = new StringBuffer("SELECT ");
        if (!tmeta.containsPK(updateColumns)) {
            // PK should be included.
            selectSQLAppender.append(getColumnNameInSQL(tmeta.getPkName()) + ", ");
        }
        for (int i = 0; i < updateColumns.size(); i++) {
            selectSQLAppender.append(updateColumns.get(i));
            if (i < (updateColumns.size() - 1)) {
                selectSQLAppender.append(", ");
            }
        }
        String whereCondition = null;
        ArrayList<Object> paramAppender = new ArrayList<>();
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = recognizer.getWhereCondition((ParametersHolder)statementProxy, paramAppender);
        } else {
            whereCondition = recognizer.getWhereCondition();
        }
        selectSQLAppender.append(" FROM " + getFromTableInSQL() + " WHERE " + whereCondition + " FOR UPDATE");
        String selectSQL = selectSQLAppender.toString();

        TableRecords beforeImage = null;
        PreparedStatement ps = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            if (paramAppender.isEmpty()) {
                st = statementProxy.getConnection().createStatement();
                rs = st.executeQuery(selectSQL);
            } else {
                ps = statementProxy.getConnection().prepareStatement(selectSQL);
                for (int i = 0; i< paramAppender.size(); i++) {
                    ps.setObject(i + 1, paramAppender.get(i));
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

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer)sqlRecognizer;

        TableMeta tmeta = getTableMeta();
        if (beforeImage == null || beforeImage.size() == 0) {
            return TableRecords.empty(getTableMeta());
        }
        List<String> updateColumns = recognizer.getUpdateColumns();

        StringBuffer selectSQLAppender = new StringBuffer("SELECT ");
        if (!tmeta.containsPK(updateColumns)) {
            // PK should be included.
            selectSQLAppender.append(getColumnNameInSQL(tmeta.getPkName()) + ", ");
        }
        for (int i = 0; i < updateColumns.size(); i++) {
            selectSQLAppender.append(updateColumns.get(i));
            if (i < (updateColumns.size() - 1)) {
                selectSQLAppender.append(", ");
            }
        }
        List<Field> pkRows = beforeImage.pkRows();
        selectSQLAppender.append(
            " FROM " + getFromTableInSQL() + " WHERE " + buildWhereConditionByPKs(pkRows) + " FOR UPDATE");
        String selectSQL = selectSQLAppender.toString();

        TableRecords afterImage = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = statementProxy.getConnection().prepareStatement(selectSQL);
            int index = 0;
            for (Field pkField : pkRows) {
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

}
