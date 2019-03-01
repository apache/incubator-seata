package com.alibaba.fescar.rm.datasource.undo;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.rm.datasource.undo.mysql.keyword.MySQLKeywordChecker;

/**
 * @author Wu
 * xingfudeshi@gmail.com
 * The Type keyword checker factory
 */
public class KeywordCheckerFactory {

    /**
     * get keyword checker
     *
     * @param dbType
     * @return
     */
    public static KeywordChecker getKeywordChecker(String dbType) {
        if (dbType.equals(JdbcConstants.MYSQL)) {
            return new MySQLKeywordChecker();
        } else {
            throw new NotSupportYetException(dbType);
        }

    }
}
