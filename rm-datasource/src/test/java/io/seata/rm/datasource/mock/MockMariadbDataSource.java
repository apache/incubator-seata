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

/**
 * @author funkye
 */
public class MockMariadbDataSource extends MockDataSource {
    @Override
    public Connection getConnection() throws SQLException {
        return new MockConnection(new MockDriver(), "jdbc:mariadb://127.0.0.1:3306/seata?rewriteBatchedStatements=true", null);
    }
}
