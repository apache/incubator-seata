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

public class MysqlResourceIdInitializer extends AbstractResourceIdInitializer {
    @Override
    public boolean supports(String dbType, DataSourceProxy proxy) {
        return JdbcConstants.MYSQL.equals(dbType) || JdbcConstants.POLARDBX.equals(dbType);
    }

    /**
     * jdbc:mysql:loadbalance://192.168.100.2:3306,192.168.100.1:3306/seata
     * @param proxy
     */
    @Override
    protected void doInitResourceId(DataSourceProxy proxy) {
        String startsWith = "jdbc:mysql:loadbalance://";
        if (proxy.getJdbcUrl().startsWith(startsWith)) {
            String url = proxy.getJdbcUrl();
            if (url.contains(JDBC_URL_SPLIT_CHAR)) {
                url = url.substring(0, url.indexOf(JDBC_URL_SPLIT_CHAR));
            }
            proxy.setResourceId(url.replace(",", "|"));
        } else {
            initDefaultResourceId(proxy);
        }
    }

    private void initDefaultResourceId(DataSourceProxy proxy) {
        String resourceId = proxy.getJdbcUrl();
        if (resourceId.contains(JDBC_URL_SPLIT_CHAR)) {
            resourceId = resourceId.substring(0, resourceId.indexOf(JDBC_URL_SPLIT_CHAR));
        }
        proxy.setResourceId(resourceId);
    }
}
