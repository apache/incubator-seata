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
package org.apache.seata.rm.datasource.mock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.exec.LockConflictException;


public class MockLockConflictConnectionProxy extends ConnectionProxy {
    /**
     * Instantiates a new Connection proxy.
     *
     * @param dataSourceProxy  the data source proxy
     * @param targetConnection the target connection
     */
    public MockLockConflictConnectionProxy(DataSourceProxy dataSourceProxy,
                                           Connection targetConnection) {
        super(dataSourceProxy, targetConnection);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        super.releaseSavepoint(savepoint);
    }

    @Override
    public void checkLock(String lockKeys) throws SQLException {
        throw new LockConflictException("mock lock conflict");
    }
}
