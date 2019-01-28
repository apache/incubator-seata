/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.test;

import java.util.Date;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.rm.datasource.DataSourceManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The type Data source basic test.
 */
@Ignore
public class DataSourceBasicTest {

    private static String TABLE_CREATE_SQL = "CREATE TABLE `user0` (\n"
        + "  `id` int(11) NOT NULL,\n"
        + "  `name` varchar(255) DEFAULT NULL,\n"
        + "  `gmt` datetime DEFAULT NULL,\n"
        + "  PRIMARY KEY (`id`)\n"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8";

    private static ClassPathXmlApplicationContext context;
    private static JdbcTemplate jdbcTemplate;
    private static JdbcTemplate directJdbcTemplate;

    /**
     * Test insert.
     */
    @Test
    public void testInsert() {
        RootContext.bind("mock.xid");
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
            new Object[] {2, "xxx", new Date()});
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate() {
        RootContext.bind("mock.xid");
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[] {1});
    }

    /**
     * Test update with alias 1.
     */
    @Test
    public void testUpdateWithAlias1() {

        directJdbcTemplate.update("delete from User1 where Id = ?",
            new Object[] {1});
        directJdbcTemplate.update("insert into User1 (Id, Name, gMt) values (?, ?, ?)",
            new Object[] {1, "xxx", new Date()});

        RootContext.bind("mock.xid");
        jdbcTemplate.update("update User1 a set a.Name = 'yyy' where a.Name = ?", new Object[] {"xxx"});
    }

    /**
     * Test update with alias.
     */
    @Test
    public void testUpdateWithAlias() {
        RootContext.bind("mock.xid");
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.name = ?", new Object[] {"yyyy"});
    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() {
        RootContext.bind("mock.xid");
        jdbcTemplate.update("delete from user0 where id = ?", new Object[] {2});
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdate() {
        RootContext.bind("mock.xid");
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
    }

    /**
     * Test select for update with alias.
     */
    @Test
    public void testSelectForUpdateWithAlias() {
        RootContext.bind("mock.xid");
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
    }

    /**
     * Before.
     */
    @BeforeClass
    public static void before() {
        // Mock DataSourceManager
        DataSourceManager.set(new DataSourceManager() {

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                                       String lockKeys) throws TransactionException {
                return 123456L;
            }

            @Override
            public void branchReport(String xid, long branchId, BranchStatus status, String applicationData)
                throws TransactionException {

            }

            @Override
            public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
                throws TransactionException {
                return true;
            }

            @Override
            public void registerResource(Resource resource) {

            }

            @Override
            public void unregisterResource(Resource resource) {

            }

            @Override
            public BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData)
                throws TransactionException {
                return BranchStatus.PhaseTwo_Committed;
            }

            @Override
            public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData)
                throws TransactionException {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        });

        context = new ClassPathXmlApplicationContext(
            "basic-test-context.xml");
        jdbcTemplate = (JdbcTemplate)context
            .getBean("jdbcTemplate");
        directJdbcTemplate = (JdbcTemplate)context
            .getBean("directJdbcTemplate");

    }

    /**
     * After.
     */
    @AfterClass
    public static void after() {
        if (context != null) {
            context.close();
        }
    }
}
