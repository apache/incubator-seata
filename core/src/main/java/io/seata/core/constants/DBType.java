/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.constants;

import io.seata.common.util.StringUtils;

/**
 * database type
 *
 * @author zhangsen
 */
public enum DBType {

    /**
     * Mysql db type.
     */
    MYSQL,

    /**
     * Oracle db type.
     */
    ORACLE,

    /**
     * Db 2 db type.
     */
    DB2,

    /**
     * Sqlserver db type.
     */
    SQLSERVER,

    /**
     * Sybaee db type.
     */
    SYBAEE,

    /**
     * H2 db type.
     */
    H2,

    /**
     * Sqlite db type.
     */
    SQLITE,

    /**
     * Access db type.
     */
    ACCESS,

    /**
     * Postgresql db type.
     */
    POSTGRESQL,

    /**
     * Oceanbase db type.
     */
    OCEANBASE,

    /**
     * Maria db type.
     */
    MARIADB,

    /**
     * JTDS db type.
     */
    JTDS,

    /**
     * HyperSQL db type.
     */
    HSQL,

    /**
     * Sybase db type.
     */
    SYBASE,

    /**
     * Derby db type.
     */
    DERBY,

    /**
     * HBase db type.
     */
    HBASE,

    /**
     * Hive db type.
     */
    HIVE,

    /**
     * DM db type.
     */
    DM,

    /**
     * Kingbase db type.
     */
    KINGBASE,

    /**
     * GBase db type.
     */
    GBASE,

    /**
     * Xugu db type.
     */
    XUGU,

    /**
     * OceanBase_Oracle db type.
     */
    OCEANBASE_ORACLE,

    /**
     * Informix db type.
     */
    INFORMIX,

    /**
     * ODPS db type.
     */
    ODPS,

    /**
     * Teradata db type.
     */
    TERADATA,

    /**
     * Log4jdbc db type.
     */
    LOG4JDBC,

    /**
     * Phoenix db type.
     */
    PHOENIX,

    /**
     * EDB db type.
     */
    EDB,

    /**
     * Kylin db type.
     */
    KYLIN,

    /**
     * Presto db type.
     */
    PRESTO,

    /**
     * Elasticsearch db type.
     */
    ELASTIC_SEARCH,

    /**
     * ClickHouse db type.
     */
    CLICKHOUSE,

    /**
     * kdb db type.
     */
    KDB,

    /**
     * PolarDB db type.
     */
    POLARDB;

    /**
     * Valueof db type.
     *
     * @param dbType the db type
     * @return the db type
     */
    public static DBType valueof(String dbType) {
        for (DBType dt : values()) {
            if (StringUtils.equalsIgnoreCase(dt.name(), dbType)) {
                return dt;
            }
        }
        throw new IllegalArgumentException("unknown dbtype:" + dbType);
    }

}
