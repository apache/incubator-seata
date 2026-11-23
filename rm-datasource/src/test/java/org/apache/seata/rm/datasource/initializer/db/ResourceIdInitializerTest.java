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
import org.apache.seata.rm.datasource.initializer.ResourceIdInitializer;
import org.apache.seata.rm.datasource.initializer.ResourceIdInitializerRegistry;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceIdInitializerTest {

    @Test
    public void testRegistryReturnsDbSpecificInitializer() {
        DataSourceProxy proxy = mock(DataSourceProxy.class);

        assertInstanceOf(
                PostgresqlResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.POSTGRESQL, proxy));
        assertInstanceOf(
                OracleResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.ORACLE, proxy));
        assertInstanceOf(
                MysqlResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.MYSQL, proxy));
        assertInstanceOf(
                MysqlResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.POLARDBX, proxy));
        assertInstanceOf(
                SqlServerResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.SQLSERVER, proxy));
        assertInstanceOf(
                DMResourceIdInitializer.class, ResourceIdInitializerRegistry.getInitializer(JdbcConstants.DM, proxy));
        assertInstanceOf(
                OscarResourceIdInitializer.class,
                ResourceIdInitializerRegistry.getInitializer(JdbcConstants.OSCAR, proxy));
    }

    @Test
    public void testRegistryFallsBackToDefaultInitializer() {
        ResourceIdInitializer initializer =
                ResourceIdInitializerRegistry.getInitializer("h2", mock(DataSourceProxy.class));

        assertInstanceOf(DefaultResourceIdInitializer.class, initializer);
    }

    @Test
    public void testDefaultInitializerStripsQueryString() {
        assertResourceId(
                new DefaultResourceIdInitializer(), "jdbc:h2:mem:seata?mode=mysql&trace=true", "jdbc:h2:mem:seata");
    }

    @Test
    public void testDefaultInitializerDoesNotSupportAnyDbTypeDirectly() {
        assertFalse(new DefaultResourceIdInitializer().supports(JdbcConstants.MYSQL, mock(DataSourceProxy.class)));
    }

    @Test
    public void testMysqlInitializerSupportsMysqlAndPolardbx() {
        MysqlResourceIdInitializer initializer = new MysqlResourceIdInitializer();

        assertTrue(initializer.supports(JdbcConstants.MYSQL, mock(DataSourceProxy.class)));
        assertTrue(initializer.supports(JdbcConstants.POLARDBX, mock(DataSourceProxy.class)));
    }

    @Test
    public void testMysqlLoadbalanceInitializerNormalizesHostsAndStripsQueryString() {
        assertResourceId(
                new MysqlResourceIdInitializer(),
                "jdbc:mysql:loadbalance://192.168.0.1:3306,192.168.0.2:3306/seata?useSSL=false",
                "jdbc:mysql:loadbalance://192.168.0.1:3306|192.168.0.2:3306/seata");
    }

    @Test
    public void testMysqlInitializerStripsQueryStringForRegularUrl() {
        assertResourceId(
                new MysqlResourceIdInitializer(),
                "jdbc:mysql://127.0.0.1:3306/seata?useSSL=false",
                "jdbc:mysql://127.0.0.1:3306/seata");
    }

    @Test
    public void testPostgresqlInitializerKeepsCurrentSchemaAndNormalizesSeparators() {
        assertResourceId(
                new PostgresqlResourceIdInitializer(),
                "jdbc:postgresql://host1:5432,host2:5432/seata?ssl=true&currentSchema=app,public&targetServerType=primary",
                "jdbc:postgresql://host1:5432|host2:5432/seata?currentSchema=app!public");
    }

    @Test
    public void testDmInitializerKeepsSchemaAndRemovesQuotes() {
        assertResourceId(
                new DMResourceIdInitializer(),
                "jdbc:dm://127.0.0.1:5236?compatibleMode=mysql&schema=\"APP\"&socketTimeout=30",
                "jdbc:dm://127.0.0.1:5236?schema=APP");
    }

    @Test
    public void testOracleInitializerAppendsUserNameAndStripsQueryString() {
        assertResourceId(
                new OracleResourceIdInitializer(),
                "jdbc:oracle:thin:@127.0.0.1:1521:orcl?remarksReporting=true",
                "SEATA",
                "jdbc:oracle:thin:@127.0.0.1:1521:orcl/SEATA");
    }

    @Test
    public void testOscarInitializerAppendsUserNameAndStripsQueryString() {
        assertResourceId(
                new OscarResourceIdInitializer(),
                "jdbc:oscar://127.0.0.1:2003/seata?characterEncoding=utf8",
                "SEATA",
                "jdbc:oscar://127.0.0.1:2003/seata/SEATA");
    }

    @Test
    public void testSqlServerInitializerKeepsOnlyResourceIdentityProperties() {
        assertResourceId(
                new SqlServerResourceIdInitializer(),
                "jdbc:sqlserver://localhost:1433;encrypt=true;databaseName=seata;INSTANCENAME=sqlexpress;"
                        + "trustServerCertificate=true",
                "jdbc:sqlserver://localhost:1433;databaseName=seata;INSTANCENAME=sqlexpress");
    }

    @Test
    public void testSqlServerInitializerKeepsBaseUrlWhenNoIdentityPropertiesExist() {
        assertResourceId(
                new SqlServerResourceIdInitializer(),
                "jdbc:sqlserver://localhost:1433;encrypt=true;trustServerCertificate=true",
                "jdbc:sqlserver://localhost:1433");
    }

    private void assertResourceId(ResourceIdInitializer initializer, String jdbcUrl, String expectedResourceId) {
        assertResourceId(initializer, jdbcUrl, null, expectedResourceId);
    }

    private void assertResourceId(
            ResourceIdInitializer initializer, String jdbcUrl, String userName, String expectedResourceId) {
        DataSourceProxy proxy = mock(DataSourceProxy.class);
        when(proxy.getJdbcUrl()).thenReturn(jdbcUrl);
        when(proxy.getUserName()).thenReturn(userName);

        initializer.initResourceId(proxy);

        verify(proxy).setResourceId(expectedResourceId);
    }
}
