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

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.initializer.AbstractResourceIdInitializer;
import org.apache.seata.sqlparser.util.JdbcConstants;

public class SqlServerResourceIdInitializer extends AbstractResourceIdInitializer {
    @Override
    public boolean supports(String dbType, DataSourceProxy proxy) {
        return JdbcConstants.SQLSERVER.equals(dbType);
    }

    /**
     * The general form of the connection URL for SqlServer is
     * jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
     * required connection properties: [INSTANCENAME], [databaseName,database]
     *
     * @param proxy
     */
    @Override
    protected void doInitResourceId(DataSourceProxy proxy) {
        String resourceId = proxy.getJdbcUrl();
        if (resourceId.contains(";")) {
            StringBuilder jdbcUrlBuilder = new StringBuilder();
            jdbcUrlBuilder.append(resourceId, 0, resourceId.indexOf(';'));
            StringBuilder paramsBuilder = getParamsBuilder(resourceId);

            if (paramsBuilder.length() > 0) {
                jdbcUrlBuilder.append(";");
                jdbcUrlBuilder.append(paramsBuilder);
            }
            resourceId = jdbcUrlBuilder.toString();
        }
        proxy.setResourceId(resourceId);
    }

    private static StringBuilder getParamsBuilder(String resourceId) {
        StringBuilder paramsBuilder = new StringBuilder();
        String paramUrl = resourceId.substring(resourceId.indexOf(';') + 1);
        String[] urlParams = paramUrl.split(";");
        boolean first = true;
        for (String urlParam : urlParams) {
            String[] paramSplit = urlParam.split("=");
            String propertyName = paramSplit[0];
            if ("INSTANCENAME".equalsIgnoreCase(propertyName)
                    || "databaseName".equalsIgnoreCase(propertyName)
                    || "database".equalsIgnoreCase(propertyName)) {
                if (!first) {
                    paramsBuilder.append(";");
                }
                paramsBuilder.append(urlParam);
                first = false;
            }
        }
        return paramsBuilder;
    }
}
