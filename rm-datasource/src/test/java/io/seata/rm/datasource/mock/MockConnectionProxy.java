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
package io.seata.rm.datasource.mock;

import java.sql.Connection;
import java.sql.SQLException;

import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;

/**
 * @author will
 */
public class MockConnectionProxy extends ConnectionProxy {
    /**
     * Instantiates a new Connection proxy.
     *
     * @param dataSourceProxy  the data source proxy
     * @param targetConnection the target connection
     */
    public MockConnectionProxy(DataSourceProxy dataSourceProxy,
                               Connection targetConnection) {
        super(dataSourceProxy, targetConnection);
    }

    @Override
    public void checkLock(String lockKeys) throws SQLException {
        //do nothing
    }
}
