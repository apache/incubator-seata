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
import com.alibaba.fescar.rm.datasource.sql.SQLDeleteRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

public class DeleteExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    public DeleteExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        SQLDeleteRecognizer visitor = (SQLDeleteRecognizer) sqlRecognizer;
        TableMeta tmeta = getTableMeta(visitor.getTableName());
        List<String> columns = new ArrayList<>();
        for (String column : tmeta.getAllColumns().keySet()) {
            columns.add(column);
        }

        StringBuffer selectSQLAppender = new StringBuffer("SELECT ");

        for (int i = 0; i < columns.size(); i++) {
            selectSQLAppender.append(columns.get(i));
            if (i < (columns.size() - 1)) {
                selectSQLAppender.append(", ");
            }
        }
        String whereCondition = null;
        ArrayList<Object> paramAppender = new ArrayList<>();
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = visitor.getWhereCondition((ParametersHolder) statementProxy, paramAppender);
        } else {
            whereCondition = visitor.getWhereCondition();
        }
        selectSQLAppender.append(" FROM " + tmeta.getTableName() + " WHERE " + whereCondition + " FOR UPDATE");
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
        return TableRecords.empty(getTableMeta());
    }
}
