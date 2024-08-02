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
import java.sql.SQLException;

/**
 * The type Data source proxy metadata.
 *
 */
public interface SeataDataSourceProxyMetadata {

    /**
     * Init datasource metadata
     * @param dataSource the datasource
     * @throws SQLException sql exception
     */
    void init(DataSource dataSource) throws SQLException;

    /**
     * Get variable value by name
     * @param name the name
     * @return value
     */
    String getVariableValue(String name);

    /**
     * Get jdbc url
     * @return jdbc url
     */
    String getJdbcUrl();

    /**
     * Gets db type.
     *
     * @return the db type
     */
    String getDbType();

    /**
     * Get database connection username
     * @return username
     */
    String getUserName();

    /**
     * Get kernel version
     * @return kernel version
     */
    String getKernelVersion();

}
