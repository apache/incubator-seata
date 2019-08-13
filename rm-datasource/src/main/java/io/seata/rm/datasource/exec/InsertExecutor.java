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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Insert executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author yuanguoyao
 * @date 2019-03-21 21:30:02
 */
public class InsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsertExecutor.class);
    protected static final String ERR_SQL_STATE = "S1009";

    /**
     * Instantiates a new Insert executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public InsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                          SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        //Pk column exists or PK is just auto generated
        List<Object> pkValues = containsPK() ? getPkValuesByColumn() : getPkValuesByAuto();

        TableRecords afterImage = buildTableRecords(pkValues);

        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }

        return afterImage;
    }

    protected boolean containsPK() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        TableMeta tmeta = getTableMeta();
        return tmeta.containsPK(insertColumns);
    }

    protected List<Object> getPkValuesByColumn() throws SQLException {
        // insert values including PK
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        String pk = getTableMeta().getPkName();
        List<Object> pkValues = null;
        if (statementProxy instanceof PreparedStatementProxy) {
            PreparedStatementProxy preparedStatementProxy = (PreparedStatementProxy) statementProxy;
            ArrayList<Object>[] paramters = preparedStatementProxy.getParameters();
            int insertColumnsSize = insertColumns.size();
            int cycleNums = paramters.length / insertColumnsSize;
            List<Integer> pkIndexs = new ArrayList<>(cycleNums);
            int firstPkIndex = 0;
            for (int paramIdx = 0; paramIdx < insertColumns.size(); paramIdx++) {
                if (insertColumns.get(paramIdx).equalsIgnoreCase(pk)) {
                    firstPkIndex = paramIdx;
                    break;
                }
            }
            for (int i = 0; i < cycleNums; i++) {
                pkIndexs.add(insertColumnsSize * i + firstPkIndex);
            }
            if (pkIndexs.size() == 1) {
                //adapter test case
                pkValues = preparedStatementProxy.getParamsByIndex(pkIndexs.get(0));
            } else {
                pkValues = pkIndexs.stream().map(pkIndex -> paramters[pkIndex].get(0)).collect(Collectors.toList());
            }
        } else {
            for (int paramIdx = 0; paramIdx < insertColumns.size(); paramIdx++) {
                if (insertColumns.get(paramIdx).equalsIgnoreCase(pk)) {
                    List<List<Object>> insertRows = recognizer.getInsertRows();
                    pkValues = new ArrayList<>(insertRows.size());
                    for (List<Object> row : insertRows) {
                        pkValues.add(row.get(paramIdx));
                    }
                    break;
                }
            }
        }
        if (pkValues == null) {
            throw new ShouldNeverHappenException();
        }
        //pk auto generated while column exists and value is null
        if (pkValues.size() == 1 && pkValues.get(0) instanceof Null) {
            pkValues = getPkValuesByAuto();
        }
        return pkValues;
    }


    protected List<Object> getPkValuesByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        if (pkMetaMap.size() != 1) {
            throw new NotSupportYetException();
        }
        ColumnMeta pkMeta = pkMetaMap.values().iterator().next();
        if (!pkMeta.isAutoincrement()) {
            throw new ShouldNeverHappenException();
        }

        ResultSet genKeys = null;
        try {
            genKeys = statementProxy.getTargetStatement().getGeneratedKeys();
        } catch (SQLException e) {
            // java.sql.SQLException: Generated keys not requested. You need to
            // specify Statement.RETURN_GENERATED_KEYS to
            // Statement.executeUpdate() or Connection.prepareStatement().
            if (ERR_SQL_STATE.equalsIgnoreCase(e.getSQLState())) {
                LOGGER.warn("Fail to get auto-generated keys, use \'SELECT LAST_INSERT_ID()\' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.");
                genKeys = statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()");
            } else {
                throw e;
            }
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }
}
