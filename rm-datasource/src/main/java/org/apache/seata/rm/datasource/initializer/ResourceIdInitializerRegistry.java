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

package org.apache.seata.rm.datasource.initializer;

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.initializer.db.DMResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.DefaultResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.MysqlResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.OracleResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.OscarResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.PostgresqlResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.db.SqlServerResourceIdInitializer;

import java.util.ArrayList;
import java.util.List;

public class ResourceIdInitializerRegistry {
    private static final List<ResourceIdInitializer> INITIALIZERS = new ArrayList<>();

    static {
        INITIALIZERS.add(new PostgresqlResourceIdInitializer());
        INITIALIZERS.add(new OracleResourceIdInitializer());
        INITIALIZERS.add(new MysqlResourceIdInitializer());
        INITIALIZERS.add(new SqlServerResourceIdInitializer());
        INITIALIZERS.add(new DMResourceIdInitializer());
        INITIALIZERS.add(new OscarResourceIdInitializer());
    }

    public static ResourceIdInitializer getInitializer(String dbType, DataSourceProxy proxy) {
        for (ResourceIdInitializer initializer : INITIALIZERS) {
            if (initializer.supports(dbType, proxy)) {
                return initializer;
            }
        }
        return new DefaultResourceIdInitializer();
    }
}
