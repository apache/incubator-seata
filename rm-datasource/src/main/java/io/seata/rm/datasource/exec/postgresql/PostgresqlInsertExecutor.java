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
package io.seata.rm.datasource.exec.postgresql;

import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public class PostgresqlInsertExecutor extends BaseInsertExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlInsertExecutor.class);

    public PostgresqlInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected List<Object> getPkValues() throws SQLException {
        return containsPK() ? getPkValuesByColumn() :
                (containsColumns() ? getPkValuesByAuto() : getPkValuesByColumn());
    }

    public List<Object> getPkValuesByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        if (pkMetaMap.size() != 1) {
            throw new NotSupportYetException();
        }
        ResultSet genKeys = statementProxy.getTargetStatement().getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }

    public List<Object> getPkValuesByColumn() throws SQLException {
        List<Object> pkValues = parsePkValuesFromStatement();
        if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
            pkValues = getPkValuesBySequence(pkValues.get(0));
        } else if (pkValues.size() == 1 && pkValues.get(0) instanceof SqlMethodExpr) {
            pkValues = getPkValuesByAuto();
        } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlDefaultExpr) {
            pkValues = getPkValuesByAuto();
        }
        return pkValues;
    }

    private List<Object> getPkValuesBySequence(Object expr) throws SQLException {
        try {
            return getPkValuesByAuto();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        ResultSet genKeys;
        if (expr instanceof SqlSequenceExpr) {
            SqlSequenceExpr sequenceExpr = (SqlSequenceExpr) expr;
            final String sql = "SELECT currval('" + sequenceExpr.getSequence() + "')";
            LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);
            genKeys = statementProxy.getConnection().createStatement().executeQuery(sql);
        } else {
            throw new NotSupportYetException(String.format("not support expr [%s]", expr.getClass().getName()));
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }

}
