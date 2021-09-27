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
package io.seata.server.lock.hbase;

import io.seata.core.store.LockDO;
import io.seata.server.storage.hbase.lock.LockStoreHBaseDao;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;


import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: HBaseLockStoreDaoTest
 * Description:
 *
 * @author haishin
 */
@SpringBootTest
public class HBaseLockStoreDaoTest {

    static LockStoreHBaseDao lockStoreHBaseDao = null;

    static Connection connection = null;


    @BeforeAll
    public static void start(ApplicationContext context) throws Exception {


        Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", "hadoop1");

        connection = ConnectionFactory.createConnection(configuration);
        lockStoreHBaseDao = new LockStoreHBaseDao();
        lockStoreHBaseDao.setHBaseConnection(connection);
        lockStoreHBaseDao.setLockTableName("seata:lockTable");
        lockStoreHBaseDao.setLockKeyTableName("seata:lockKey");
        lockStoreHBaseDao.setLockCF("lock");
        lockStoreHBaseDao.setTransactionIdCF("transactionId");
    }

    @Test
    public void test_acquireLocks() {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = lockStoreHBaseDao.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        // query lock information
        List<LockDO> queryList = lockStoreHBaseDao.queryLockDOs(lockDOs);
        // Inserted successfully
        if (queryList.size() == 3)
            Assertions.assertTrue(true);
        else
            Assertions.assertTrue(false);


        Assertions.assertTrue(lockStoreHBaseDao.unLock(lockDOs));

    }

    @Test
    public void test_re_acquireLocks() {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = lockStoreHBaseDao.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        // query lock information
        List<LockDO> queryList = lockStoreHBaseDao.queryLockDOs(lockDOs);
        // Inserted successfully
        if (queryList.size() == 3)
            Assertions.assertTrue(true);
        else
            Assertions.assertTrue(false);

        //lock again
        Assertions.assertTrue(lockStoreHBaseDao.acquireLock(lockDOs));

        Assertions.assertTrue(lockStoreHBaseDao.unLock(lockDOs));
    }

    @Test
    public void tes_unLocks() {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-456:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = lockStoreHBaseDao.acquireLock(lockDOs);
        Assertions.assertTrue(ret);
        // query lock information
        List<LockDO> queryList = lockStoreHBaseDao.queryLockDOs(lockDOs);
        // Inserted successfully
        if (queryList.size() > 0)
            Assertions.assertTrue(true);
        else
            Assertions.assertTrue(false);

        //unlock
        Assertions.assertTrue(lockStoreHBaseDao.unLock(lockDOs));
        // query lock information
        List<LockDO> againQueryList = lockStoreHBaseDao.queryLockDOs(lockDOs);
        // Inserted successfully
        if (againQueryList.size() == 0)
            Assertions.assertTrue(true);
        else
            Assertions.assertTrue(false);

    }

    @Test
    public void test_isLockable_can() {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-678:123");
            lock.setTransactionId(123L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = lockStoreHBaseDao.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        //unlock
        Assertions.assertTrue(lockStoreHBaseDao.unLock(lockDOs));
    }

    @Test
    public void test_isLockable_cannot() {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:222");
            lock.setTransactionId(222L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs.add(lock);
        }

        boolean ret = lockStoreHBaseDao.acquireLock(lockDOs);
        Assertions.assertTrue(ret);

        // query lock information
        List<LockDO> queryList = lockStoreHBaseDao.queryLockDOs(lockDOs);
        // Inserted successfully
        if (queryList.size() > 0)
            Assertions.assertTrue(true);
        else
            Assertions.assertTrue(false);

        List<LockDO> lockDOs_2 = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            LockDO lock = new LockDO();
            lock.setResourceId("abc");
            lock.setXid("abc-123:333");
            lock.setTransactionId(333L);
            lock.setBranchId((long) i);
            lock.setRowKey("abc-" + i);
            lock.setPk(String.valueOf(i));
            lock.setTableName("t");
            lockDOs_2.add(lock);
        }

        boolean ret2 = lockStoreHBaseDao.acquireLock(lockDOs_2);
        Assertions.assertTrue(!ret2);
    }


    @Test
    public void test_unlock() {
        lockStoreHBaseDao.unLock("abc-123:786756", 5657L);

    }

}
