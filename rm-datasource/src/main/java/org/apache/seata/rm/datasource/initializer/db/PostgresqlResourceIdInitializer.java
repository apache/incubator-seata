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

package org.apache.seata.rm.datasource.initializer.db;

import org.apache.seata.common.Constants;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.initializer.AbstractResourceIdInitializer;
import org.apache.seata.sqlparser.util.JdbcConstants;

public class PostgresqlResourceIdInitializer extends AbstractResourceIdInitializer {
    @Override
    public boolean supports(String dbType, DataSourceProxy proxy) {
        return JdbcConstants.POSTGRESQL.equals(dbType);
    }

    /**
     * jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=public
     * jdbc:postgresql://127.0.0.1:5432/seata?currentSchema=seata
     *
     * @param proxy
     */
    @Override
    protected void doInitResourceId(DataSourceProxy proxy) {
        String resourceId = proxy.getJdbcUrl();
        if (resourceId.contains(JDBC_URL_SPLIT_CHAR)) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(resourceId, 0, resourceId.indexOf(JDBC_URL_SPLIT_CHAR));

            StringBuilder paramsBuilder = new StringBuilder();
            String paramUrl = resourceId.substring(resourceId.indexOf(JDBC_URL_SPLIT_CHAR) + 1);
            String[] urlParams = paramUrl.split("&");
            for (String urlParam : urlParams) {
                if (urlParam.contains("currentSchema")) {
                    if (urlParam.contains(Constants.DBKEYS_SPLIT_CHAR)) {
                        urlParam = urlParam.replace(Constants.DBKEYS_SPLIT_CHAR, "!");
                    }
                    paramsBuilder.append(urlParam);
                    break;
                }
            }

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append(JDBC_URL_SPLIT_CHAR);
                jdbcUrlBuilder.append(paramsBuilder);
            }
            resourceId = jdbcUrlBuilder.toString();
        }
        if (resourceId.contains(",")) {
            resourceId = resourceId.replace(",", "|");
        }
        proxy.setResourceId(resourceId);
    }
}
