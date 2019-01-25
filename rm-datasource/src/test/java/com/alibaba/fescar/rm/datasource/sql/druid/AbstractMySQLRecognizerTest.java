package com.alibaba.fescar.rm.datasource.sql.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.List;

/**
 * @author hanwen
 * created at 2019-01-25
 */
public class AbstractMySQLRecognizerTest {

    public SQLStatement getSQLStatement(String sql) {
        List<SQLStatement> stats = SQLUtils.parseStatements(sql, "mysql");
        return stats.get(0);
    }

}
