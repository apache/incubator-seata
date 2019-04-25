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
package io.seata.test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.rm.datasource.DataSourceManager;
import io.seata.tm.TMClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The type Data source basic test.
 *
 * @author sharajava
 */
@Ignore
public class DataSourceBasicTest {

    private static ClassPathXmlApplicationContext context;
    private static JdbcTemplate jdbcTemplate;
    private static JdbcTemplate directJdbcTemplate;

    private static final String APPLICATION_ID = "my_test_app";
    private static final String TX_SERVICE_GROUP = "my_test_tx_group";
    private static final long GID = 12345678L;
    private static final AtomicLong INC_LONG = new AtomicLong(1);

    private static void initClient() {
        TMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
        RMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
    }

    /**
     * Before.
     */
    @BeforeClass
    public static void before() {
        initClient();
        // Mock DataSourceManager
        DefaultResourceManager.mockResourceManager(BranchType.AT, new DataSourceManager() {

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                                       String applicationData, String lockKeys) throws TransactionException {
                return GID + INC_LONG.incrementAndGet();
            }

            @Override
            public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                                     String applicationData) {

            }

            @Override
            public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) {
                return true;
            }

            @Override
            public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                             String applicationData) {
                return BranchStatus.PhaseTwo_Committed;
            }

            @Override
            public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                               String applicationData) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        });

        context = new ClassPathXmlApplicationContext(
            "basic-test-context.xml");
        jdbcTemplate = (JdbcTemplate)context
            .getBean("jdbcTemplate");
        directJdbcTemplate = (JdbcTemplate)context
            .getBean("directJdbcTemplate");
        RootContext.bind("127.0.0.1:8091:" + GID);
        directJdbcTemplate.update("delete from undo_log");

    }

    /**
     * After.
     */
    @AfterClass
    public static void after() {
        RootContext.unbind();
        if (context != null) {
            context.close();
        }
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate() {
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[] {1});
    }

    /**
     * Test update with alias.
     */
    @Test
    public void testUpdateWithAlias() {
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.name = ?", new Object[] {"yyyy"});
    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() {
        jdbcTemplate.update("delete from user0 where id = ?", new Object[] {2});
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdate() {
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
    }

    /**
     * Test select for update with alias.
     */
    @Test
    public void testSelectForUpdateWithAlias() {
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
    }

    /**
     * Test insert.
     */
    @Test
    public void testInsert() {
        directJdbcTemplate.update("delete from user0 where id = ?",
            new Object[] {2});
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
            new Object[] {2, "xxx", new Date()});
    }

    /**
     * Test update with alias 1.
     */
    @Test
    public void testUpdateWithAlias1() {

        directJdbcTemplate.update("delete from user1 where Id = ?",
            new Object[] {1});
        directJdbcTemplate.update("insert into user1 (Id, Name, gMt) values (?, ?, ?)",
            new Object[] {1, "xxx", new Date()});
        jdbcTemplate.update("update user1 a set a.Name = 'yyy' where a.Name = ?", new Object[] {"xxx"});
    }
}
