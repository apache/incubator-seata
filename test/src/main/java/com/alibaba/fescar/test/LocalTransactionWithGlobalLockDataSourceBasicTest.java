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

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.rm.datasource.DataSourceManager;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The type Data source basic test.
 */
@Ignore
public class LocalTransactionWithGlobalLockDataSourceBasicTest {

    private static ClassPathXmlApplicationContext context;
    private static JdbcTemplate jdbcTemplate;
    private static JdbcTemplate directJdbcTemplate;

    private abstract static class LockConflictExecuteTemplate {
        public void execute() {
            synchronized (LocalTransactionWithGlobalLockDataSourceBasicTest.class) {
                DataSourceManager.set(new MockDataSourceManager() {
                    @Override
                    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
                        throws TransactionException {
                        return false;
                    }
                });

                boolean exceptionOccour = false;
                try {
                    doExecute();
                } catch (UncategorizedSQLException e) {
                    exceptionOccour = true;
                    Assert.assertTrue("not lock Conflict exception", e.getMessage().contains("LockConflict"));
                } finally {
                    DataSourceManager.set(new MockDataSourceManager());
                }

                Assert.assertTrue("Lock Exception not occur!", exceptionOccour);
            }
        }

        public abstract void doExecute();

    }

    /**
     * Test insert.
     */
    @Test
    public void testInsert() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
            new Object[] {2, "xxx", new Date()});
    }

    /**
     * Test insert.
     */
    @Test
    public void testInsertWithLock() {
        RootContext.bindGlobalLockFlag();
        new LockConflictExecuteTemplate() {
            @Override
            public void doExecute() {
                jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
                    new Object[] {3, "xxx", new Date()});
            }
        }.execute();
    }

    /**
     * Test update.
     */
    @Test
    public void testUpdate() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[] {1});
    }

    @Test
    public void testUpdateWithLock() {
        RootContext.bindGlobalLockFlag();
        RootContext.bindGlobalLockFlag();
        new LockConflictExecuteTemplate() {
            @Override
            public void doExecute() {
                jdbcTemplate.update("update user0 a set a.name = 'yyyy' where a.id = ?", new Object[] {1});

            }
        }.execute();
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

        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("update User1 a set a.Name = 'yyy' where a.Name = ?", new Object[] {"xxx"});
    }

    @Test
    public void testUpdateWithAlias1WithLockConflict() {

        directJdbcTemplate.update("delete from User1 where Id = ?",
            new Object[] {1});
        directJdbcTemplate.update("insert into User1 (Id, Name, gMt) values (?, ?, ?)",
            new Object[] {1, "xxx", new Date()});

        RootContext.bindGlobalLockFlag();
        new LockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.update("update User1 a set a.Name = 'yyy' where a.Name = ?", new Object[] {"xxx"});
            }
        };

    }

    /**
     * Test delete.
     */
    @Test
    public void testDelete() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.update("delete from user0 where id = ?", new Object[] {2});
    }

    /**
     * Test delete.
     */
    @Test
    public void testDeleteForLockConflict() {
        RootContext.bindGlobalLockFlag();
        new LockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.update("delete from user0 where id = ?", new Object[] {2});
            }
        };
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdate() {
        RootContext.bindGlobalLockFlag();
        jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
    }

    /**
     * Test select for update.
     */
    @Test
    public void testSelectForUpdateWithLockConflict() {
        RootContext.bindGlobalLockFlag();
        new LockConflictExecuteTemplate() {

            @Override
            public void doExecute() {
                jdbcTemplate.queryForRowSet("select a.name from user0 a where a.id = ? for update", new Object[] {1});
            }
        };
    }

    public static class MockDataSourceManager extends DataSourceManager {

        @Override
        public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,String applicationData,
                                   String lockKeys) throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }

        @Override
        public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData)
            throws TransactionException {
            throw new RuntimeException("this method should not be called!");
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
        public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
            throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }

        @Override
        public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
            throws TransactionException {
            throw new RuntimeException("this method should not be called!");
        }
    }

    /**
     * Before.
     */
    @BeforeClass
    public static void before() {
        // Mock DataSourceManager
        DataSourceManager.set(new MockDataSourceManager());

        context = new ClassPathXmlApplicationContext(
            "basic-test-context.xml");
        jdbcTemplate = (JdbcTemplate)context
            .getBean("jdbcTemplate");
        directJdbcTemplate = (JdbcTemplate)context
            .getBean("directJdbcTemplate");

        directJdbcTemplate.execute("delete from user0");
        directJdbcTemplate.execute("delete from user1");

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
