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
package org.apache.seata.server.lock.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.LockDO;
import org.apache.seata.server.storage.db.lock.LockStoreDataBaseDAO;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.store.fs.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 */
@SpringBootTest
public class DataBaseLockStoreDAOTest {

    static LockStoreDataBaseDAO dataBaseLockStoreDAO  = null;

    static BasicDataSource dataSource = null;

    @BeforeAll
    public static void start(ApplicationContext context){
        dataSource =  new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:./db_store/lock");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.STORE_DB_TYPE, "h2");
        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.LOCK_DB_TABLE, "lock_table");
        dataBaseLockStoreDAO = new LockStoreDataBaseDAO(dataSource);

        prepareTable(dataSource);
    }

    private static void prepareTable(BasicDataSource dataSource) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            try {
                stmt.execute("drop table lock_table");
            } catch (Exception e) {
            }
            stmt.execute("CREATE TABLE lock_table ( xid varchar(96) ,  transaction_id long , branch_id long, resource_id varchar(32) ,table_name varchar(32) ,pk varchar(32) ,  row_key  varchar(128) primary key not null , status  integer , gmt_create TIMESTAMP(6) ,gmt_modified TIMESTAMP(6) ) ");
            System.out.println("create table lock_table success.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(stmt, conn);
        }
    }

    @Test
    public void test_acquireLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("test_acquireLocks-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:123' and table_name  = 't' " +
                "and row_key in ('test_acquireLocks-0','test_acquireLocks-1','test_acquireLocks-2')"  ;
        Connection conn =  null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.assertTrue(true);
            } else {
                Assertions.fail();
            }
        } finally {
            IOUtil.close(rs, conn);
        }

        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

    }


    @Test
    public void test_re_acquireLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("test_re_acquireLocks-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:123' and table_name  = 't' " +
                "and row_key in ('test_re_acquireLocks-0','test_re_acquireLocks-1','test_re_acquireLocks-2')"  ;
        Connection conn =  null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.assertTrue(true);
            } else {
                Assertions.fail();
            }
        } finally {
            IOUtil.close(rs, conn);
        }

        //lock again
        Assertions.assertTrue(dataBaseLockStoreDAO.acquireLock(lockDOs));

        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

    }

    @Test
    public void tes_unLocks() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-456:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("tes_unLocks-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-456:123' and table_name  = 't' " +
                "and row_key in ('tes_unLocks-0','tes_unLocks-1','tes_unLocks-2')"  ;
        Connection conn =  null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.assertTrue(true);
            } else {
                Assertions.fail();
            }
            rs.close();

            //unlock
            Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));

            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.fail();
            } else {
                Assertions.assertTrue(true);
            }
        } finally {
            IOUtil.close(rs, conn);
        }


    }


    @Test
    public void test_isLockable_can(){
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-678:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("test_isLockable_can-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        //unlock
        Assertions.assertTrue(dataBaseLockStoreDAO.unLock(lockDOs));
    }

    @Test
    public void test_isLockable_cannot() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:222");
            lock.setTransactionId(222L);
            lock.setBranchId((long) i);
            lock.setRowKey("test_isLockable_cannot-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:222' and table_name  = 't' " +
                "and row_key in ('test_isLockable_cannot-0','test_isLockable_cannot-1','test_isLockable_cannot-2')"  ;
        Connection conn =  null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.assertTrue(true);
            } else {
                Assertions.fail();
            }
        } finally {
            IOUtil.close(rs, conn);
        }

        List<LockDO> lockDOs_2 = new ArrayList<>();
        for(int i = 0; i < 3; i++){
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:333");
            lock.setTransactionId(333L);
            lock.setBranchId((long) i);
            lock.setRowKey("test_isLockable_cannot-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs_2.add(lock);
        }

        boolean ret2 = dataBaseLockStoreDAO.acquireLock(lockDOs_2);
        Assertions.assertFalse(ret2);

    }

    @Test
    public void test_isLockable_cannot1() throws SQLException {
        List<LockDO> lockDOs = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:222");
            lock.setTransactionId(222L);
            lock.setBranchId(1L);
            lock.setRowKey("test_isLockable_cannot1-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = dataBaseLockStoreDAO.acquireLock(lockDOs, true, true);
        Assertions.assertTrue(ret);

        String sql = "select * from lock_table where xid = 'abc-123:222' and table_name  = 't' " +
                "and row_key in ('test_isLockable_cannot1-0','test_isLockable_cannot1-1','test_isLockable_cannot1-2')"  ;
        Connection conn =  null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            rs = conn.createStatement().executeQuery(sql);
            if (rs.next()) {
                Assertions.assertTrue(true);
            } else {
                Assertions.fail();
            }
        } finally {
            IOUtil.close(rs, conn);
        }

        List<LockDO> lockDOs_2 = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:333");
            lock.setTransactionId(333L);
            lock.setBranchId(2L);
            lock.setRowKey("test_isLockable_cannot1-"+i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs_2.add(lock);
        }

        boolean ret2 = dataBaseLockStoreDAO.acquireLock(lockDOs_2, true, true);
        Assertions.assertFalse(ret2);

    }

    @AfterAll
    public static void clearStoreDB(){
        FileUtils.deleteRecursive("db_store", true);
    }

}
