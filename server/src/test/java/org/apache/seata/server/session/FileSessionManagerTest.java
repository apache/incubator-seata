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
package org.apache.seata.server.session;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.XID;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.server.console.service.BranchSessionService;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.util.StoreUtil;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.apache.seata.server.session.SessionHolder.CONFIG;

/**
 * The type File based session manager test.
 *
 * @since 2019 /1/22
 */
@SpringBootTest
public class FileSessionManagerTest {


    private static volatile List<SessionManager> sessionManagerList;

    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

    @Resource(type = BranchSessionService.class)
    private BranchSessionService branchSessionService;

    private static String sessionStorePath = CONFIG.getConfig(ConfigurationKeys.STORE_FILE_DIR,
            DEFAULT_SESSION_STORE_FILE_DIR);

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        StoreUtil.deleteDataFile();
        try {
            EnhancedServiceLoader.unloadAll();
            sessionManagerList =
                Arrays.asList(new FileSessionManager("root.data", "."), new FileSessionManager("test", null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void addGlobalSessionTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Find global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void findGlobalSessionTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            GlobalSession expected = sessionManager.findGlobalSession(globalSession.getXid());
            Assertions.assertNotNull(expected);
            Assertions.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
            Assertions.assertEquals(expected.getApplicationId(), globalSession.getApplicationId());
            Assertions.assertEquals(expected.getTransactionServiceGroup(), globalSession.getTransactionServiceGroup());
            Assertions.assertEquals(expected.getTransactionName(), globalSession.getTransactionName());
            Assertions.assertEquals(expected.getTransactionId(), globalSession.getTransactionId());
            Assertions.assertEquals(expected.getStatus(), globalSession.getStatus());
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Update global session status test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void updateGlobalSessionStatusTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            globalSession.setStatus(GlobalStatus.Finished);
            sessionManager.updateGlobalSessionStatus(globalSession, GlobalStatus.Finished);
            GlobalSession expected = sessionManager.findGlobalSession(globalSession.getXid());
            Assertions.assertNotNull(expected);
            Assertions.assertEquals(GlobalStatus.Finished, expected.getStatus());
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Remove global session test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void removeGlobalSessionTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            sessionManager.removeGlobalSession(globalSession);
            GlobalSession expected = sessionManager.findGlobalSession(globalSession.getXid());
            Assertions.assertNull(expected);
        }
    }

    /**
     * Add branch session test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void addBranchSessionTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            sessionManager.addBranchSession(globalSession, branchSession);
            sessionManager.removeBranchSession(globalSession, branchSession);
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Update branch session status test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void updateBranchSessionStatusTest(GlobalSession globalSession, BranchSession branchSession)
            throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            sessionManager.addBranchSession(globalSession, branchSession);
            sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseTwo_Committed);
            sessionManager.removeBranchSession(globalSession, branchSession);
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * Remove branch session test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void removeBranchSessionTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.addGlobalSession(globalSession);
            sessionManager.addBranchSession(globalSession, branchSession);
            sessionManager.removeBranchSession(globalSession, branchSession);
            sessionManager.removeGlobalSession(globalSession);
        }
    }

    /**
     * All sessions test.
     *
     * @param globalSessions the global sessions
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionsProvider")
    public void allSessionsTest(List<GlobalSession> globalSessions) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            for (GlobalSession globalSession : globalSessions) {
                sessionManager.addGlobalSession(globalSession);
            }
            Collection<GlobalSession> expectedGlobalSessions = sessionManager.allSessions();
            Assertions.assertNotNull(expectedGlobalSessions);
            Assertions.assertEquals(2, expectedGlobalSessions.size());
            for (GlobalSession globalSession : globalSessions) {
                sessionManager.removeGlobalSession(globalSession);
            }
        }
    }

    /**
     * Find global sessions test.
     *
     * @param globalSessions the global sessions
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionsProvider")
    public void findGlobalSessionsTest(List<GlobalSession> globalSessions) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            for (GlobalSession globalSession : globalSessions) {
                sessionManager.addGlobalSession(globalSession);
            }
            SessionCondition sessionCondition = new SessionCondition(30 * 24 * 3600);
            Collection<GlobalSession> expectedGlobalSessions = sessionManager.findGlobalSessions(sessionCondition);
            Assertions.assertNotNull(expectedGlobalSessions);
            Assertions.assertEquals(2, expectedGlobalSessions.size());

            SessionCondition sessionCondition1 = new SessionCondition(globalSessions.get(0).getXid());
            expectedGlobalSessions = sessionManager.findGlobalSessions(sessionCondition1);
            Assertions.assertNotNull(expectedGlobalSessions);
            Assertions.assertEquals(1, expectedGlobalSessions.size());

            sessionCondition1.setTransactionId(globalSessions.get(0).getTransactionId());
            expectedGlobalSessions = sessionManager.findGlobalSessions(sessionCondition1);
            Assertions.assertNotNull(expectedGlobalSessions);
            Assertions.assertEquals(1, expectedGlobalSessions.size());

            sessionCondition1.setStatuses(globalSessions.get(0).getStatus());
            expectedGlobalSessions = sessionManager.findGlobalSessions(sessionCondition1);
            Assertions.assertNotNull(expectedGlobalSessions);
            Assertions.assertEquals(1, expectedGlobalSessions.size());

            for (GlobalSession globalSession : globalSessions) {
                sessionManager.removeGlobalSession(globalSession);
            }

        }
    }

    /**
     * Find global sessions with PageResult test.
     *
     * @param globalSessions the global sessions
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionsWithPageResultProvider")
    public void findGlobalSessionsWithPageResultTest(List<GlobalSession> globalSessions) throws Exception {
        SessionHolder.getRootSessionManager().destroy();
        SessionHolder.init(SessionMode.FILE);

        try {
            SessionHolder.init(SessionMode.FILE);
            final SessionManager sessionManager = SessionHolder.getRootSessionManager();
            // make sure sessionMaanager is empty
            Collection<GlobalSession> sessions = sessionManager.allSessions();
            if (CollectionUtils.isNotEmpty(sessions)) {
                // FileSessionManager use ConcurrentHashMap is thread safe
                for (GlobalSession session : sessions) {
                    sessionManager.removeGlobalSession(session);
                }
            }
            for (GlobalSession globalSession : globalSessions) {
                globalSession.begin();
            }
            final GlobalSessionParam globalSessionParam = new GlobalSessionParam();

            // wrong pageSize or pageNum
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> globalSessionService.query(globalSessionParam)
            );

            // page
            globalSessionParam.setPageSize(1);
            globalSessionParam.setPageNum(1);
            final PageResult<GlobalSessionVO> sizeAndNumTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(1, sizeAndNumTestResult.getCurrPage());
            Assertions.assertEquals(3, sizeAndNumTestResult.getPages());
            Assertions.assertEquals(1, sizeAndNumTestResult.getData().size());
            Assertions.assertEquals(3, sizeAndNumTestResult.getTotal());

            // xid
            final GlobalSession firstGlobalSession = globalSessions.get(0);
            globalSessionParam.setXid(firstGlobalSession.getXid());
            final PageResult<GlobalSessionVO> xidTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(1, xidTestResult.getData().size());
            Assertions.assertEquals(globalSessionParam.getXid(), xidTestResult.getData().get(0).getXid());

            // transaction name
            globalSessionParam.setXid(null);
            globalSessionParam.setTransactionName("test2");
            final PageResult<GlobalSessionVO> transactionNameTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(1, transactionNameTestResult.getData().size());
            Assertions.assertEquals(globalSessionParam.getTransactionName(),
                    transactionNameTestResult.getData().get(0).getTransactionName());

            // application id
            globalSessionParam.setPageSize(3);
            globalSessionParam.setTransactionName(null);
            globalSessionParam.setApplicationId("demo-app");
            final PageResult<GlobalSessionVO> applicationIdTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(2, applicationIdTestResult.getData().size());
            Assertions.assertEquals(
                    globalSessionParam.getApplicationId(),
                    applicationIdTestResult.getData()
                            .stream()
                            .map(GlobalSessionVO::getApplicationId)
                            .distinct()
                            .reduce(String::concat).orElse("")
            );

            // status
            globalSessionParam.setApplicationId(null);
            globalSessionParam.setWithBranch(true);
            globalSessionParam.setStatus(GlobalStatus.CommitFailed.getCode());
            final PageResult<GlobalSessionVO> statusTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(0, statusTestResult.getData().size());

            // with branch
            globalSessionParam.setStatus(null);
            final PageResult<GlobalSessionVO> withBranchTestResult = globalSessionService.query(globalSessionParam);
            Assertions.assertEquals(3, withBranchTestResult.getData().size());
            Assertions.assertEquals(3, withBranchTestResult.getData().size());

            // timeStart and timeEnd

            globalSessionParam.setWithBranch(false);
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), 1).getTime());
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), -1).getTime());
            Assertions.assertEquals(0, globalSessionService.query(globalSessionParam).getData().size());


            globalSessionParam.setTimeStart(null);
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeEnd(DateUtils.addHours(new Date(), 1).getTime());
            Assertions.assertEquals(0, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), -1).getTime());
            Assertions.assertEquals(0, globalSessionService.query(globalSessionParam).getData().size());
        } finally {
            for (GlobalSession globalSession : globalSessions) {
                globalSession.end();
            }
            SessionHolder.destroy();
        }

    }

    /**
     * On begin test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void onBeginTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    /**
     * On status change test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void onStatusChangeTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onStatusChange(globalSession, GlobalStatus.Finished);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    @ParameterizedTest
    @MethodSource("globalSessionForLockTestProvider")
    public void stopGlobalSessionTest(List<GlobalSession> globalSessions) throws Exception {
        try {
            SessionHolder.init(SessionMode.FILE);
            for (GlobalSession globalSession : globalSessions) {
                globalSession.begin();
            }
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    globalSessionService.stopGlobalRetry(globalSessions.get(0).getXid()));

            GlobalSession globalSession = globalSessions.get(1);
            globalSession.changeGlobalStatus(GlobalStatus.CommitRetrying);
            String xid = globalSession.getXid();
            globalSessionService.stopGlobalRetry(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.StopCommitOrCommitRetry);

            globalSession.changeGlobalStatus(GlobalStatus.RollbackRetrying);
            globalSessionService.stopGlobalRetry(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.StopRollbackOrRollbackRetry);
        } finally {
            for (GlobalSession globalSession : globalSessions) {
                globalSession.setStatus(GlobalStatus.Committed);
                globalSession.end();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("globalSessionForLockTestProvider")
    public void changeGlobalSessionTest(List<GlobalSession> globalSessions) throws Exception {
        try {
            SessionHolder.init(SessionMode.FILE);
            for (GlobalSession globalSession : globalSessions) {
                globalSession.begin();
            }
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    globalSessionService.changeGlobalStatus(globalSessions.get(0).getXid()));

            GlobalSession globalSession = globalSessions.get(1);
            globalSession.changeGlobalStatus(GlobalStatus.CommitFailed);
            String xid = globalSession.getXid();
            globalSessionService.changeGlobalStatus(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.CommitRetrying);

            globalSession.changeGlobalStatus(GlobalStatus.RollbackFailed);
            globalSessionService.changeGlobalStatus(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.RollbackRetrying);
        } finally {
            for (GlobalSession globalSession : globalSessions) {
                globalSession.setStatus(GlobalStatus.Committed);
                globalSession.end();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("globalSessionForLockTestProvider")
    public void startGlobalSessionTest(List<GlobalSession> globalSessions) throws Exception {
        try {
            SessionHolder.init(SessionMode.FILE);
            for (GlobalSession globalSession : globalSessions) {
                globalSession.begin();
            }
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    globalSessionService.startGlobalRetry(globalSessions.get(0).getXid()));

            GlobalSession globalSession = globalSessions.get(1);
            globalSession.setStatus(GlobalStatus.StopCommitOrCommitRetry);
            String xid = globalSession.getXid();
            globalSessionService.startGlobalRetry(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.CommitRetrying);

            globalSession.setStatus(GlobalStatus.StopRollbackOrRollbackRetry);
            globalSessionService.startGlobalRetry(xid);
            Assertions.assertEquals(SessionHolder.findGlobalSession(xid).getStatus(),
                    GlobalStatus.RollbackRetrying);
        } finally {
            for (GlobalSession globalSession : globalSessions) {
                globalSession.setStatus(GlobalStatus.Committed);
                globalSession.end();
            }
        }
    }

    /**
     * On branch status change test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void onBranchStatusChangeTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onAddBranch(globalSession, branchSession);
            sessionManager.onBranchStatusChange(globalSession, branchSession, BranchStatus.PhaseTwo_Committed);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    /**
     * On add branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void onAddBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onAddBranch(globalSession, branchSession);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    /**
     * On remove branch test.
     *
     * @param globalSession the global session
     * @param branchSession the branch session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("branchSessionProvider")
    public void onRemoveBranchTest(GlobalSession globalSession, BranchSession branchSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onAddBranch(globalSession, branchSession);
            sessionManager.onRemoveBranch(globalSession, branchSession);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    /**
     * On close test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void onCloseTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onClose(globalSession);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    @ParameterizedTest
    @MethodSource("branchSessionsProvider")
    public void stopBranchRetryTest(GlobalSession globalSession) throws Exception {
        try {
            SessionHolder.init(SessionMode.FILE);
            globalSession.begin();
            // wrong param for xid and branchId
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> branchSessionService.stopBranchRetry("xid", null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> branchSessionService.stopBranchRetry(globalSession.getXid(), "test"));

            // wrong status for branch transaction
            List<BranchSession> branchSessions = globalSession.getBranchSessions();
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> branchSessionService.stopBranchRetry(globalSession.getXid(),
                            String.valueOf(branchSessions.get(0).getBranchId())));

            // wrong status for global transaction
            globalSession.setStatus(GlobalStatus.Begin);
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> branchSessionService.stopBranchRetry(globalSession.getXid(),
                            String.valueOf(branchSessions.get(1).getBranchId())));

            // success stop
            globalSession.setStatus(GlobalStatus.CommitRetrying);
            branchSessionService.stopBranchRetry(globalSession.getXid(), String.valueOf(branchSessions.get(1).getBranchId()));
            GlobalSession newGlobalSession = SessionHolder.findGlobalSession(globalSession.getXid());
            Assertions.assertEquals(BranchStatus.STOP_RETRY, newGlobalSession.getBranchSessions().get(1).getStatus());
        } finally {
            globalSession.setStatus(GlobalStatus.Committed);
            globalSession.end();
        }
    }

    @ParameterizedTest
    @MethodSource("branchSessionsProvider")
    public void restartBranchFailRetryTest(GlobalSession globalSession) throws Exception {
        try {
            SessionHolder.init(SessionMode.FILE);
            globalSession.begin();
            List<BranchSession> branchSessions = globalSession.getBranchSessions();
            // wrong status for branch transaction
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> branchSessionService.startBranchRetry(globalSession.getXid(),
                            String.valueOf(branchSessions.get(0).getBranchId())));
            // success
            branchSessionService.startBranchRetry(globalSession.getXid(),
                    String.valueOf(branchSessions.get(2).getBranchId()));
            GlobalSession newGlobalSession = SessionHolder.findGlobalSession(globalSession.getXid());
            Assertions.assertEquals(BranchStatus.Registered, newGlobalSession.getBranchSessions().get(2).getStatus());
        } finally {
            globalSession.setStatus(GlobalStatus.Committed);
            globalSession.end();
        }
    }

    /**
     * On end test.
     *
     * @param globalSession the global session
     * @throws Exception the exception
     */
    @ParameterizedTest
    @MethodSource("globalSessionProvider")
    public void onEndTest(GlobalSession globalSession) throws Exception {
        for (SessionManager sessionManager : sessionManagerList) {
            sessionManager.onBegin(globalSession);
            sessionManager.onSuccessEnd(globalSession);
        }
    }

    /**
     * Global session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> globalSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test", 6000);

        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);

        return Stream.of(
                Arguments.of(globalSession)
        );
    }

    /**
     * Global sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> globalSessionsProvider() {
        GlobalSession globalSession1 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test", 6000);
        GlobalSession globalSession2 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test", 6000);
        return Stream.of(
                Arguments.of(Arrays.asList(globalSession1, globalSession2))
        );
    }

    /**
     * Global sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> globalSessionsWithPageResultProvider() throws ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        GlobalSession globalSession1 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test1", 60000);
        globalSession1.setBeginTime(dateFormat.parse("2220-1-1 08:02:00").getTime());

        GlobalSession globalSession2 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test2", 60000);
        globalSession2.setBeginTime(dateFormat.parse("2220-1-1 08:04:00").getTime());

        GlobalSession globalSession3 = new GlobalSession("with-branchSession-app", DEFAULT_TX_GROUP, "test3", 60000);
        globalSession3.setBeginTime(dateFormat.parse("2220-1-1 08:20:00").getTime());
        globalSession3.setStatus(GlobalStatus.CommitFailed);


        final BranchSession branchSession = new BranchSession();
        branchSession.setApplicationData("applicationData");
        branchSession.setResourceGroupId("applicationData");
        branchSession.setClientId("clientId");
        branchSession.setResourceId("resourceId");
        branchSession.setLockKey("lockKey");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setTransactionId(11L);
        branchSession.setBranchId(22L);
        branchSession.setXid("xid");
        branchSession.setLockStatus(LockStatus.Locked);
        globalSession3.add(branchSession);


        return Stream.of(
                Arguments.of(Arrays.asList(globalSession1, globalSession2, globalSession3))
        );
    }

    static Stream<Arguments> globalSessionForLockTestProvider() throws ParseException {
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setTransactionId(UUIDGenerator.generateUUID());
        branchSession1.setBranchId(1L);
        branchSession1.setClientId("c1");
        branchSession1.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession1.setResourceId("department");
        branchSession1.setLockKey("a:1,2");
        branchSession1.setBranchType(BranchType.AT);
        branchSession1.setApplicationData("{\"data\":\"test\"}");
        branchSession1.setBranchType(BranchType.AT);

        BranchSession branchSession2 = new BranchSession();
        branchSession2.setTransactionId(UUIDGenerator.generateUUID());
        branchSession2.setBranchId(2L);
        branchSession2.setClientId("c1");
        branchSession2.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession2.setResourceId("department");
        branchSession2.setLockKey("e:3,4");
        branchSession2.setBranchType(BranchType.AT);
        branchSession2.setApplicationData("{\"data\":\"test\"}");
        branchSession2.setBranchType(BranchType.AT);

        branchSession1.setTransactionId(397215L);
        branchSession2.setTransactionId(92482L);

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        GlobalSession globalSession1 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test1", 6000);
        globalSession1.setXid("xid1");
        globalSession1.add(branchSession1);
        globalSession1.setBeginTime(dateFormat.parse("2022-1-1 03:00:00").getTime());

        GlobalSession globalSession2 = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test2", 6000);
        globalSession2.setXid("ddd1");
        globalSession2.add(branchSession2);
        globalSession2.setBeginTime(dateFormat.parse("2022-1-1 08:00:00").getTime());

        return Stream.of(Arguments.of(Arrays.asList(globalSession1, globalSession2)));
    }

    /**
     * Branch session provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test", 6000);
        globalSession.setXid(XID.generateXID(globalSession.getTransactionId()));
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        return Stream.of(
                Arguments.of(globalSession, branchSession)
        );
    }

    /**
     * Branch sessions provider object [ ] [ ].
     *
     * @return the object [ ] [ ]
     */
    static Stream<Arguments> branchSessionsProvider() {
        GlobalSession globalSession = new GlobalSession("demo-app", DEFAULT_TX_GROUP, "test", 6000);
        globalSession.setXid(XID.generateXID(globalSession.getTransactionId()));
        globalSession.setStatus(GlobalStatus.CommitRetrying);
        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(1L);
        branchSession.setXid(globalSession.getXid());
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setStatus(BranchStatus.PhaseOne_Failed);
        branchSession.setBranchType(BranchType.AT);
        BranchSession branchSession1 = new BranchSession();
        branchSession1.setBranchId(2L);
        branchSession1.setXid(globalSession.getXid());
        branchSession1.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession1.setStatus(BranchStatus.Registered);
        branchSession1.setBranchType(BranchType.AT);
        BranchSession branchSession2 = new BranchSession();
        branchSession2.setBranchId(3L);
        branchSession2.setXid(globalSession.getXid());
        branchSession2.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession2.setStatus(BranchStatus.STOP_RETRY);
        branchSession2.setBranchType(BranchType.AT);
        globalSession.add(branchSession);
        globalSession.add(branchSession1);
        globalSession.add(branchSession2);
        return Stream.of(Arguments.of(globalSession));
    }
}
