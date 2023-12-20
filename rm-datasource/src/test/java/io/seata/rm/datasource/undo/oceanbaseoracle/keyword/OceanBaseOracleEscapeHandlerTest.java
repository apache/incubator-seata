package io.seata.rm.datasource.undo.oceanbaseoracle.keyword;

import io.seata.sqlparser.EscapeHandler;
import io.seata.sqlparser.EscapeHandlerFactory;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OceanBaseOracleEscapeHandlerTest {

    @Test
    public void testOceanBaseOracleEscapeHandlerTest() {
        EscapeHandler escapeHandler = EscapeHandlerFactory.getEscapeHandler(JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertNotNull(escapeHandler);
    }

}