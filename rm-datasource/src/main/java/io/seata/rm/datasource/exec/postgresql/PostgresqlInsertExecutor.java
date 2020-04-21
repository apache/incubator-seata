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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public class PostgresqlInsertExecutor extends BaseInsertExecutor implements Sequenceable, Defaultable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlInsertExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public PostgresqlInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                                    SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected List<Object> getPkValues() throws SQLException {
        return containsPK() ? getPkValuesByColumn() :
                (containsColumns() ? getGeneratedKeys() : getPkValuesByColumn());
    }

    public List<Object> getPkValuesByColumn() throws SQLException {
        List<Object> pkValues = parsePkValuesFromStatement();
        if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
            pkValues = getPkValuesBySequence((SqlSequenceExpr) pkValues.get(0));
        } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlMethodExpr) {
            pkValues = getGeneratedKeys();
        } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlDefaultExpr) {
            pkValues = getPkValuesByDefault();
        }
        return pkValues;
    }

    /**
     * get primary key values by default
     * @return
     * @throws SQLException
     */
    public List<Object> getPkValuesByDefault() throws SQLException {
        // current version 1.2 only support postgresql.
        // mysql default keyword the logic not support. (sample: insert into test(id, name) values(default, 'xx'))
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        ColumnMeta pkMeta = pkMetaMap.values().iterator().next();
        String columnDef = pkMeta.getColumnDef();
        // sample: nextval('test_id_seq'::regclass)
        String seq = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "'", "'");
        String function = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "", "(");
        if (StringUtils.isBlank(seq)) {
            throw new ShouldNeverHappenException("get primary key value failed, cause columnDef is " + columnDef);
        }
        return getPkValuesBySequence(new SqlSequenceExpr("'" + seq + "'", function));
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT currval(" + expr.getSequence() + ")";
    }
}
