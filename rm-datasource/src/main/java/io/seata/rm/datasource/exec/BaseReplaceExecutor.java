package io.seata.rm.datasource.exec;

import io.seata.rm.datasource.StatementProxy;
import io.seata.sqlparser.SQLRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.List;

/**
 * @author jingliu_xiong@foxmail.com
 */
public abstract class BaseReplaceExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> implements ReplaceExecutor<T>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseReplaceExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseReplaceExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                              SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }
}
