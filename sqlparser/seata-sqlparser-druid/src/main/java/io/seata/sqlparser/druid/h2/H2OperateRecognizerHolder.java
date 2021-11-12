package io.seata.sqlparser.druid.h2;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;
/**
 * @author hongyan
 * @date Created in 2021-11-09 17:06
 * @description
 */
@LoadLevel(name = JdbcConstants.H2)
public class H2OperateRecognizerHolder implements SQLOperateRecognizerHolder {

    public H2OperateRecognizerHolder() {
    }

    @Override
    public SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast) {
        return new H2DeleteRecognizer(sql, ast);
    }
    @Override
    public SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast) {
        return new H2InsertRecognizer(sql, ast);
    }
    @Override
    public SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast) {
        return new H2UpdateRecognizer(sql, ast);
    }
    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast) {
        return ((SQLSelectStatement)ast).getSelect().getFirstQueryBlock().isForUpdate() ? new H2SelectForUpdateRecognizer(sql, ast) : null;
    }
}

