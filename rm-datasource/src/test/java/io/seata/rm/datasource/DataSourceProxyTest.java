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
package io.seata.rm.datasource;

import java.lang.reflect.Field;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.mock.MockDataSource;
import io.seata.rm.datasource.mock.MockDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author ph3636
 */
public class DataSourceProxyTest {

    @Test
    public void test_constructor() {
        DataSource dataSource = new MockDataSource();

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        Assertions.assertEquals(dataSourceProxy.getTargetDataSource(), dataSource);

        DataSourceProxy dataSourceProxy2 = new DataSourceProxy(dataSourceProxy);
        Assertions.assertEquals(dataSourceProxy2.getTargetDataSource(), dataSource);
    }

    @Test
    public void getResourceIdTest() throws SQLException, NoSuchFieldException, IllegalAccessException {
        MockDriver mockDriver = new MockDriver();
        String username = "username";

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);
        dataSource.setUsername(username);
        dataSource.setPassword("password");

        DataSourceProxy proxy = new DataSourceProxy(dataSource);
        Field resourceId = proxy.getClass().getDeclaredField("resourceId");
        resourceId.setAccessible(true);
        resourceId.set(proxy, null);
        Field dbTypeField = proxy.getClass().getDeclaredField("dbType");
        dbTypeField.setAccessible(true);
        dbTypeField.set(proxy, io.seata.sqlparser.util.JdbcConstants.ORACLE);

        String userName = dataSource.getConnection().getMetaData().getUserName();
        Assertions.assertEquals(userName, username);
        Field userNameField = proxy.getClass().getDeclaredField("userName");
        userNameField.setAccessible(true);
        userNameField.set(proxy, username);

        Assertions.assertEquals(proxy.getResourceId(), "jdbc:mock:xxx/username");

        dbTypeField.set(proxy, io.seata.sqlparser.util.JdbcConstants.MYSQL);
        resourceId.setAccessible(true);
        resourceId.set(proxy, null);
        Assertions.assertEquals(proxy.getResourceId(), "jdbc:mock:xxx");
        resourceId.setAccessible(true);
        resourceId.set(proxy, null);
        Field jdbUrl = proxy.getClass().getDeclaredField("jdbcUrl");
        jdbUrl.setAccessible(true);
        jdbUrl.set(proxy, "jdbc:mysql:loadbalance://192.168.100.2:3306,192.168.100.3:3306,192.168.100.1:3306/seata");
        resourceId.setAccessible(true);
        resourceId.set(proxy, null);
        Assertions.assertEquals(proxy.getResourceId(), "jdbc:mysql:loadbalance://192.168.100.2:3306|192.168.100.3:3306|192.168.100.1:3306/seata");

    }
}
