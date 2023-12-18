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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Insert executor for OceanBaseOracle
 *
 * @author hsien999
 */
@LoadLevel(name = JdbcConstants.OCEANBASE_ORACLE, scope = Scope.PROTOTYPE)
public class OceanBaseOracleInsertExecutor<T, S extends Statement> extends BaseInsertExecutor<T, S>
    implements Sequenceable, Defaultable {

    public OceanBaseOracleInsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                         SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return super.beforeImage();
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return super.afterImage(beforeImage);
    }

    /**
     * Support for multiple primary keys, and adapt to the case that contains partial pks in the inserted columns
     * Note: Oracle only supports a single value list for `values` clause in `insert`(without `all`).
     *
     * @return a mapping of primary keys to lists of corresponding values
     * @throws SQLException the sql exception
     */
    @Override
    public Map<String, List<Object>> getPkValues() throws SQLException {
        // table: test; columns: c1, c2, c3; pk: (c1, c2)
        // case1: all pks are filled.
        //     e.g. insert into test values(null, seq.nextval, 3)
        // case2: some generated pks column value are not present, and other pks are present.
        //     e.g. insert into test(c2, c3) values(2, 3), c1 is generated key
        Map<String, List<Object>> pkValuesMap = getPkValuesByColumn();
        List<String> pkColumnNames = getTableMeta().getPrimaryKeyOnlyName();
        for (String pkName : pkColumnNames) {
            if (!pkValuesMap.containsKey(pkName)) {
                pkValuesMap.put(pkName, Collections.singletonList(getGeneratedKeys(pkName).get(0)));
            }
        }
        return pkValuesMap;
    }

    @Override
    public Map<String, List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String, List<Object>> pkValuesMap = parsePkValuesFromStatement();
        for (Map.Entry<String, List<Object>> entry : pkValuesMap.entrySet()) {
            String pkKey = entry.getKey();
            if (pkKey.isEmpty()) {
                continue;
            }
            List<Object> pkValues = entry.getValue(); // assert pkValues.size() = 1
            for (int i = 0; i < pkValues.size(); ++i) {
                if (pkValues.get(i) instanceof SqlSequenceExpr) {
                    // 1. first match the sequence (assume using .nextval)
                    pkValues.set(i, getPkValuesBySequence((SqlSequenceExpr) pkValues.get(i), pkKey).get(0));
                } else if (pkValues.get(i) instanceof SqlMethodExpr) {
                    // 2. match the method
                    pkValues.set(i, getGeneratedKeys(pkKey).get(0));
                } else if (pkValues.get(i) instanceof Null) {
                    // 3. match null (e.g. sequence+trigger for pk)
                    pkValues.set(i, getGeneratedKeys(pkKey).get(0));
                } else if (pkValues.get(0) instanceof SqlDefaultExpr) {
                    // 4. not support default for pk yet
                    pkValuesMap.put(pkKey, getPkValuesByDefault());
                }
            }
            pkValuesMap.put(pkKey, pkValues);
        }
        return pkValuesMap;
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT " + expr.getSequence() + ".currval FROM DUAL";
    }

    @Override
    public List<Object> getPkValuesByDefault() {
        return getPkValuesByDefault(null);
    }

    @Override
    public List<Object> getPkValuesByDefault(String pkKey) {
        throw new NotSupportYetException("Default value is not supported yet");
    }
}
