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
package io.seata.rm.datasource.exec.oceanbaseoracle;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.AbstractDMLBaseExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi insert executor for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleMultiInsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private final String tableName;

    public OceanBaseOracleMultiInsertExecutor(StatementProxy<S> statementProxy,
                                              StatementCallback<T, S> statementCallback,
                                              List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
        boolean isAllSameTable = sqlRecognizers.stream()
            .collect(Collectors.groupingBy(SQLRecognizer::getTableName))
            .entrySet()
            .size() == 1;
        if (!isAllSameTable) {
            throw new ShouldNeverHappenException("Multi executor only supports sql recognizer for the same table source");
        }
        this.tableName = sqlRecognizers.get(0).getTableName();
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta(tableName));
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableRecords tableRecords = new TableRecords(getTableMeta(tableName));
        Connection conn = statementProxy.getConnection();
        try (Statement statement = conn.createStatement()) {
            for (SQLRecognizer recognizer : sqlRecognizers) {
                getAfterImageFromRecognizer(
                    (SQLInsertRecognizer) recognizer, beforeImage, statement, tableRecords
                );
            }
        } catch (ClassCastException e) {
            throw new ShouldNeverHappenException("Unmatched recognizer for the multi insert executor");
        }
        return tableRecords;
    }

    private void getAfterImageFromRecognizer(final SQLInsertRecognizer recognizer,
                                             final TableRecords beforeImage,
                                             final Statement statement,
                                             final TableRecords tableRecords) throws SQLException {
        String conditionSQL = recognizer.getConditionSQL();
        try (ResultSet rs = statement.executeQuery(conditionSQL)) {
            int executeTimes = 1;
            if (conditionSQL != null) {
                rs.last();
                executeTimes = rs.getRow();
                rs.beforeFirst();
            }
            for (int i = 0; i < executeTimes; ++i) {
                OceanBaseOracleInsertExecutor<T, S> executor =
                    new OceanBaseOracleInsertExecutor<>(statementProxy, statementCallback, sqlRecognizers.get(0));
                TableRecords itemRecord = executor.afterImage(beforeImage);
                itemRecord.getRows().forEach(tableRecords::add);
            }
        }
    }
}
