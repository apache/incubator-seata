package io.seata.sqlparser.druid;

import io.seata.sqlparser.util.JdbcConstants;

/**
 * A db type adapter for druid parser.
 *
 * @author hsien999
 **/
class DruidDbTypeAdapter {
    /**
     * Get adaptive db type for druid parser.
     *
     * @param dbType origin db type
     * @return adaptive db type
     */
    static String getAdaptiveDbType(String dbType) {
        if (JdbcConstants.POLARDBX.equals(dbType)) {
            return JdbcConstants.MYSQL;
        }
        return dbType;
    }
}
