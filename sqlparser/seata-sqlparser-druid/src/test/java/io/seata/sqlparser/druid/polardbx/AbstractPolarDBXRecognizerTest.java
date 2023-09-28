package io.seata.sqlparser.druid.polardbx;

import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * Base Test for recognizer of PolarDB-X
 *
 * @author hsien999
 **/
public class AbstractPolarDBXRecognizerTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        // return JdbcConstants.POLARDBX;
        return JdbcConstants.MYSQL; // adaptive to polardb-x
    }
}
