package io.seata.rm.datasource;

import io.seata.rm.datasource.exec.*;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;

import java.sql.Statement;

/**
 * @author pengzhengfa
 */
public class FactorySqlExecutor {

    public static <T, S extends Statement> Executor<T>
    SqlCreateExecutor(SQLType sqlType,
                      SQLRecognizer sqlRecognizer,
                      StatementProxy<S> statementProxy,
                      StatementCallback<T, S> statementCallback) {
        Executor<T> executor;
        switch (sqlRecognizer.getSQLType()) {
            case INSERT:
                executor = new InsertExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                break;
            case UPDATE:
                executor = new UpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                break;
            case DELETE:
                executor = new DeleteExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                break;
            case SELECT_FOR_UPDATE:
                executor = new SelectForUpdateExecutor<>(statementProxy, statementCallback, sqlRecognizer);
                break;
            default:
                executor = new PlainExecutor<>(statementProxy, statementCallback);
                break;
        }
        return executor;
    }
}