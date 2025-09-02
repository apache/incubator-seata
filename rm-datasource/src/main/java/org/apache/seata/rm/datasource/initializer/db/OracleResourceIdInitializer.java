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

public class OracleResourceIdInitializer extends AbstractResourceIdInitializer {
    @Override
    public boolean supports(String dbType, DataSourceProxy proxy) {
        return JdbcConstants.ORACLE.equals(dbType);
    }

    @Override
    protected void doInitResourceId(DataSourceProxy proxy) {
        String resourceId = proxy.getJdbcUrl();
        if (proxy.getJdbcUrl().contains(JDBC_URL_SPLIT_CHAR)) {
            resourceId = proxy.getJdbcUrl().substring(0, proxy.getJdbcUrl().indexOf(JDBC_URL_SPLIT_CHAR));
        }
        resourceId = resourceId + "/" + proxy.getUserName();
        proxy.setResourceId(resourceId);
    }
}
