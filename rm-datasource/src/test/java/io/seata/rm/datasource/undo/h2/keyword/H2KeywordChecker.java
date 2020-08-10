package io.seata.rm.datasource.undo.h2.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.mysql.keyword.MySQLKeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author JerryYin
 */
@LoadLevel(name = JdbcConstants.H2)
public class H2KeywordChecker extends MySQLKeywordChecker {
}
