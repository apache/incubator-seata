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

public class DefaultResourceIdInitializer extends AbstractResourceIdInitializer {
    @Override
    protected void doInitResourceId(DataSourceProxy proxy) {
        String jdbcUrl = proxy.getJdbcUrl();
        String resourceId = jdbcUrl;
        if (jdbcUrl.contains(JDBC_URL_SPLIT_CHAR)) {
            resourceId = jdbcUrl.substring(0, jdbcUrl.indexOf(JDBC_URL_SPLIT_CHAR));
        }
        proxy.setResourceId(resourceId);
    }

    @Override
    public boolean supports(String dbType, DataSourceProxy proxy) {
        return false;
    }
}
