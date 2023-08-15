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
package io.seata.server.lock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.annotation.Resource;

import io.seata.common.util.CollectionUtils;
import io.seata.console.result.PageResult;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.server.UUIDGenerator;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.vo.GlobalLockVO;
import io.seata.server.lock.file.FileLockManagerForTest;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import io.seata.server.session.SessionManager;
import io.seata.server.store.StoreConfig.SessionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;

/**
 * The type Lock manager test.
 *
 * @author tianming.xm @gmail.com
 * @author miaoxueyu
 * @since 2019 /1/23
 */
@SpringBootTest
public class LockManagerTest {


    @Resource(type = GlobalLockService.class)
    private GlobalLockService globalLockService;

    @BeforeAll
    public static void setUp(ApplicationContext context){

    }

    /**
     * Acquire lock success.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void acquireLock_success(BranchSession branchSession) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession));
    }

    /**
     * Acquire lock failed.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionsProvider")
    public void acquireLock_failed(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession1));
        Assertions.assertFalse(lockManager.acquireLock(branchSession2));
    }

    /**
     * deadlock test.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("deadlockBranchSessionsProvider")
    public void deadlockTest(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            new Thread(() -> {
                try {
                    lockManager.acquireLock(branchSession1);
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    lockManager.acquireLock(branchSession2);
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            // Assume execute more than 5 seconds means deadlock happened.
            Assertions.assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
        } finally {
            lockManager.releaseLock(branchSession1);
            lockManager.releaseLock(branchSession2);
        }

    }

    /**
     * Make sure two concurrent branchSession register process with different row key list, at least one process could
     * success and only one process could success.
     *
     * @param branchSession1 the branch session 1
     * @param branchSession2 the branch session 2
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("deadlockBranchSessionsProvider")
    public void concurrentUseAbilityTest(BranchSession branchSession1, BranchSession branchSession2)  throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        try {
            final AtomicBoolean first = new AtomicBoolean();
            final AtomicBoolean second = new AtomicBoolean();
            CountDownLatch countDownLatch = new CountDownLatch(2);
            new Thread(() -> {
                try {
                    first.set(lockManager.acquireLock(branchSession1));
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            new Thread(() -> {
                try {
                    second.set(lockManager.acquireLock(branchSession2));
                } catch (TransactionException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
            // Assume execute more than 5 seconds means deadlock happened.
            if (countDownLatch.await(5, TimeUnit.SECONDS)) {
                Assertions.assertTrue(!first.get() || !second.get());
            }
        } finally {
            lockManager.releaseLock(branchSession1);
            lockManager.releaseLock(branchSession2);
        }
    }

    /**
     * Is lockable test.
     *
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void isLockableTest(BranchSession branchSession) throws Exception {
        branchSession.setLockKey("t:4");
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
        lockManager.acquireLock(branchSession);
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        Assertions.assertFalse(lockManager
                .isLockable(branchSession.getXid(), branchSession.getResourceId(), branchSession.getLockKey()));
    }

    @ParameterizedTest
    @MethodSource("duplicatePkBranchSessionsProvider")
    public void duplicatePkBranchSessionHolderTest(BranchSession branchSession1, BranchSession branchSession2) throws Exception {
        LockManager lockManager = new FileLockManagerForTest();
        Assertions.assertTrue(lockManager.acquireLock(branchSession1));
        Assertions.assertEquals(4, (long) branchSession1.getLockHolder().values().size());
        Assertions.assertTrue(lockManager.releaseLock(branchSession1));
        Assertions.assertEquals(0, (long) branchSession1.getLockHolder().values().size());
        Assertions.assertTrue(lockManager.acquireLock(branchSession2));
        Assertions.assertEquals(4, (long) branchSession2.getLockHolder().values().size());
        Assertions.assertTrue(lockManager.releaseLock(branchSession2));
        Assertions.assertEquals(0, (long) branchSession2.getLockHolder().values().size());
    }

    @ParameterizedTest
    @MethodSource("globalSessionForLockTestProvider")
    public void lockQueryTest(GlobalSession globalSessions1, GlobalSession globalSessions2) throws TransactionException, ParseException {
        SessionHolder.getRootSessionManager().destroy();
        SessionHolder.init(SessionMode.FILE);
        final SessionManager sessionManager = SessionHolder.getRootSessionManager();
        //make sure sessionMaanager is empty
        Collection<GlobalSession> sessions = sessionManager.allSessions();
        if (CollectionUtils.isNotEmpty(sessions)) {
            //FileSessionManager use ConcurrentHashMap is thread safe
            for (GlobalSession session : sessions) {
                sessionManager.removeGlobalSession(session);
            }
        }
        try {
            sessionManager.addGlobalSession(globalSessions1);
            sessionManager.addGlobalSession(globalSessions2);

            final GlobalLockParam param = new GlobalLockParam();


            // wrong pageSize or pageNum
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> globalLockService.query(param)
            );

            LockManager lockManager = new FileLockManagerForTest();
            for (BranchSession branchSession : globalSessions1.getBranchSessions()) {
                lockManager.acquireLock(branchSession);
            }

            for (BranchSession branchSession : globalSessions2.getBranchSessions()) {
                lockManager.acquireLock(branchSession);
            }

            param.setPageNum(1);
            param.setPageSize(10);

            // query all data
            final PageResult<GlobalLockVO> fullQueryTestResult = globalLockService.query(param);
            Assertions.assertEquals(1,fullQueryTestResult.getPages());
            Assertions.assertEquals(8,fullQueryTestResult.getTotal());
            Assertions.assertEquals(8,fullQueryTestResult.getData().size());

            // test paging
            param.setPageSize(1);
            final PageResult<GlobalLockVO> pagingTestResult = globalLockService.query(param);
            Assertions.assertEquals(8, pagingTestResult.getPages());
            Assertions.assertEquals(8, pagingTestResult.getTotal());
            Assertions.assertEquals(1, pagingTestResult.getData().size());

            // transaction id
            param.setPageSize(10);
            param.setTransactionId("49");
            final PageResult<GlobalLockVO> transactionIdTestResult1 = globalLockService.query(param);
            Assertions.assertEquals(2, transactionIdTestResult1.getTotal());

            param.setTransactionId("72");
            final PageResult<GlobalLockVO> transactionIdTestResult2 = globalLockService.query(param);
            Assertions.assertEquals(6, transactionIdTestResult2.getTotal());

            param.setTransactionId("493747292");
            final PageResult<GlobalLockVO> transactionIdTestResult3 = globalLockService.query(param);
            Assertions.assertEquals(2, transactionIdTestResult3.getTotal());

            // branch id
            param.setTransactionId(null);
            param.setBranchId("2");
            final PageResult<GlobalLockVO> branchIdTestResult = globalLockService.query(param);
            Assertions.assertEquals(1, branchIdTestResult.getPages());
            Assertions.assertEquals(4, branchIdTestResult.getTotal());
            Assertions.assertEquals(4, branchIdTestResult.getData().size());

            // xid
            param.setBranchId(null);
            param.setXid("id1");
            final PageResult<GlobalLockVO> xidTestResult1 = globalLockService.query(param);
            Assertions.assertEquals(4, xidTestResult1.getTotal());

            param.setXid("d");
            final PageResult<GlobalLockVO> xidTestResult2 = globalLockService.query(param);
            Assertions.assertEquals(8, xidTestResult2.getTotal());

            // table name
            param.setXid(null);
            param.setTableName("de");
            final PageResult<GlobalLockVO> tableTestResult1 = globalLockService.query(param);
            Assertions.assertEquals(2, tableTestResult1.getTotal());

            param.setTableName("e");
            final PageResult<GlobalLockVO> tableTestResult2 = globalLockService.query(param);
            Assertions.assertEquals(4, tableTestResult2.getTotal());

            // timeStart and timeEnd
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            param.setTableName(null);
            param.setTimeStart(dateFormat.parse("2022-1-1 08:00:01").getTime());
            final PageResult<GlobalLockVO> timeTestResult1 = globalLockService.query(param);
            Assertions.assertEquals(0, timeTestResult1.getTotal());

            param.setTimeStart(dateFormat.parse("2022-1-1 08:00:00").getTime());
            final PageResult<GlobalLockVO> timeTestResult2 = globalLockService.query(param);
            Assertions.assertEquals(4, timeTestResult2.getTotal());

            param.setTimeStart(null);
            param.setTimeEnd(dateFormat.parse("2022-1-1 02:59:59").getTime());
            final PageResult<GlobalLockVO> timeTestResult3 = globalLockService.query(param);
            Assertions.assertEquals(0, timeTestResult3.getTotal());

            param.setTimeEnd(dateFormat.parse("2022-1-1 03:00:00").getTime());
            final PageResult<GlobalLockVO> timeTestResult4 = globalLockService.query(param);
            Assertions.assertEquals(4, timeTestResult4.getTotal());

            //test release lock
            for (BranchSession branchSession : globalSessions1.getBranchSessions()) {
                lockManager.releaseLock(branchSession);
            }

            final GlobalLockParam param2 = new GlobalLockParam();
            param2.setPageNum(1);
            param2.setPageSize(10);

            final PageResult<GlobalLockVO> fullQueryTestResult2 = globalLockService.query(param2);
            Assertions.assertEquals(1,fullQueryTestResult2.getPages());
            Assertions.assertEquals(4,fullQueryTestResult2.getTotal());
            Assertions.assertEquals(4,fullQueryTestResult2.getData().size());

        } finally {
            sessionManager.removeGlobalSession(globalSessions1);
            sessionManager.removeGlobalSession(globalSessions2);
            sessionManager.destroy();
        }
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(UUIDGenerator.generateUUID());
        branchSession.setBranchId(0L);
        branchSession.setClientId("c1");
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t:0");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setBranchType(BranchType.AT);
        return Stream.of(
                Arguments.of(branchSession));
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static BranchSession[] baseBranchSession(String resource, String lockKey1, String lockKey2) {
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setTransactionId(UUIDGenerator.generateUUID());
        branchSession1.setBranchId(1L);
        branchSession1.setClientId("c1");
        branchSession1.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession1.setResourceId(resource);
        branchSession1.setLockKey(lockKey1);
        branchSession1.setBranchType(BranchType.AT);
        branchSession1.setApplicationData("{\"data\":\"test\"}");
        branchSession1.setBranchType(BranchType.AT);

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setTransactionId(UUIDGenerator.generateUUID());
        branchSession2.setBranchId(2L);
        branchSession2.setClientId("c1");
        branchSession2.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession2.setResourceId(resource);
        branchSession2.setLockKey(lockKey2);
        branchSession2.setBranchType(BranchType.AT);
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setBranchType(BranchType.AT);

        return new BranchSession[]{branchSession1, branchSession2};
    }

    /**
     * global sessions provider object [ ] [ ].
     * @return the objects [ ] [ ]
     */
    static Stream<Arguments> globalSessionForLockTestProvider() throws ParseException {
        final BranchSession[] branchSessions1 = baseBranchSession("department", "a:1,2", "e:1,2");

        final BranchSession branchSession1 = branchSessions1[0];
        branchSession1.setTransactionId(39721L);
        final BranchSession branchSession2 = branchSessions1[1];
        branchSession2.setTransactionId(89721L);

        final BranchSession[] branchSessions2 = baseBranchSession("employee", "de:43,99", "df:33,66");
        final BranchSession branchSession3 = branchSessions2[0];
        branchSession3.setTransactionId(924823L);

        final BranchSession branchSession4 = branchSessions2[1];
        branchSession4.setTransactionId(493747292L);


        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        GlobalSession globalSession1 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test1", 6000);
        globalSession1.setXid("xid1");
        globalSession1.add(branchSession1);
        globalSession1.add(branchSession2);
        globalSession1.setBeginTime(dateFormat.parse("2022-1-1 03:00:00").getTime());

        GlobalSession globalSession2 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test2", 6000);
        globalSession2.setXid("ddd1");
        globalSession2.add(branchSession3);
        globalSession2.add(branchSession4);
        globalSession2.setBeginTime(dateFormat.parse("2022-1-1 08:00:00").getTime());

        return Stream.of(Arguments.of(globalSession1, globalSession2));
    }

    /**
     * Base branch sessions provider object [ ] [ ]. Could assign resource and lock keys.
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> baseBranchSessionsProvider(String resource, String lockKey1, String lockKey2) {

        return Stream.of(Arguments.of(baseBranchSession(resource, lockKey1, lockKey2)));
    }

    /**
     * Branch sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionsProvider() {
        return baseBranchSessionsProvider("tb_1", "t:1,2", "t:1,2");
    }

    static Stream<Arguments> deadlockBranchSessionsProvider() {
        return baseBranchSessionsProvider("tb_2", "t:1,2,3,4,5", "t:5,4,3,2,1");
    }

    static Stream<Arguments> duplicatePkBranchSessionsProvider() {
        return baseBranchSessionsProvider("tb_2", "t:1,2;t1:1;t2:2", "t:1,2;t1:1;t2:2");
    }
}
