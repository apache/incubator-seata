package io.seata.rm.datasource.exec.sqlserver;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.UpdateExecutor;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * @author GoodBoyCoder
 * @date 2021-07-21
 */
@LoadLevel(name = JdbcConstants.SQLSERVER, scope = Scope.PROTOTYPE)
public class SqlServerUpdateExecutor extends UpdateExecutor {
    /**
     * Instantiates a new SqlServer Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SqlServerUpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected StringJoiner buildStringJoinerSql(ArrayList paramAppenderList) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        StringBuilder prefix = new StringBuilder("SELECT ");
        StringBuilder suffix = new StringBuilder(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(WHERE).append(whereCondition);
        }
        return new StringJoiner(", ", prefix.toString(), suffix.toString());
    }
}
