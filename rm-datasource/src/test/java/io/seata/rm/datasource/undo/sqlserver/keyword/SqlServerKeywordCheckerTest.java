package io.seata.rm.datasource.undo.sqlserver.keyword;

import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type sqlserver sql keyword checker test.
 *
 * @author GoodBoyCoder
 */
public class SqlServerKeywordCheckerTest {
    @Test
    public void testSqlServerKeywordChecker() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.SQLSERVER);
        Assertions.assertNotNull(keywordChecker);
    }
}
