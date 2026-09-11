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
package io.seata.at;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcUtils;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;

/**
 * author doubleDimple
 */
public class DruidDataSourceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DruidDataSourceUtils.class);

    public static final int ORACLE = 1;
    public static final int POSTGRESQL = 2;

    /**
     * oracle: test1: url:jdbc:oracle:thin:@localhost:49161:xe name:system password:oracle
     *
     * test2: jdbc:oracle:thin:@localhost:1521:helowin name:system password:helowin
     */
    public static final String ORACLE_JDBC_URL = "jdbc:oracle:thin:@localhost:49161:xe";
    public static final String ORACLE_USERNAME = "system";
    public static final String ORACLE_PASSWORD = "oracle";
    public static final String ORACLE_DRIVER_CLASSNAME = JdbcUtils.ORACLE_DRIVER;

    /**
     * PostgreSQL:
     *
     */
    public static final String POSTGRESQL_JDBC_URL = "";
    public static final String POSTGRESQL_USERNAME = "";
    public static final String POSTGRESQL_PASSWORD = "";
    public static final String POSTGRESQL_DRIVER_CLASSNAME = JdbcUtils.POSTGRESQL_DRIVER;

    public static DruidDataSource createNewDruidDataSource(int type) throws Throwable {
        DruidDataSource druidDataSource = new DruidDataSource();
        switch (type) {
            case ORACLE:
                initDruidDataSourceOracle(druidDataSource);
                break;
            case POSTGRESQL:
                initDruidDataSourcePostGreSql(druidDataSource);
                break;
            default:
                throw new NotSupportYetException("unknow datasource");
        }
        return druidDataSource;
    }

    private static void initDruidDataSourcePostGreSql(DruidDataSource druidDataSource) throws SQLException {
        druidDataSource.setDbType(JdbcConstants.POSTGRESQL);
        druidDataSource.setUrl(POSTGRESQL_JDBC_URL);
        druidDataSource.setUsername(POSTGRESQL_USERNAME);
        druidDataSource.setPassword(POSTGRESQL_PASSWORD);
        druidDataSource.setDriverClassName(POSTGRESQL_DRIVER_CLASSNAME);
        druidDataSource.init();
        LOGGER.info("datasource init success");
    }

    private static void initDruidDataSourceOracle(DruidDataSource druidDataSource) throws Throwable {
        druidDataSource.setDbType(JdbcConstants.ORACLE);
        druidDataSource.setUrl(ORACLE_JDBC_URL);
        druidDataSource.setUsername(ORACLE_USERNAME);
        druidDataSource.setPassword(ORACLE_PASSWORD);
        druidDataSource.setDriverClassName(ORACLE_DRIVER_CLASSNAME);
        druidDataSource.init();
        LOGGER.info("datasource init success");
    }

}
