/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.seata.rm.datasource.metadata.DefaultDataSourceProxyMeta;
import org.apache.seata.rm.datasource.metadata.MySQLDataSourceProxyMetadata;
import org.apache.seata.rm.datasource.metadata.OracleDataSourceProxyMetadata;
import org.apache.seata.rm.datasource.util.JdbcUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;

public class SeataDataSourceProxyMetadataFactory {

    public static SeataDataSourceProxyMetadata create(DataSource dataSource) throws SQLException {
        SeataDataSourceProxyMetadata dataSourceProxyMetadata = null;
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            String dbType = JdbcUtils.getDbType(jdbcUrl);
            if (JdbcConstants.MYSQL.equals(dbType)) {
                dataSourceProxyMetadata = new MySQLDataSourceProxyMetadata();
            } else {
                dataSourceProxyMetadata = new DefaultDataSourceProxyMeta();
            }
        }
        dataSourceProxyMetadata.init(dataSource);
        return dataSourceProxyMetadata;
    }

}
