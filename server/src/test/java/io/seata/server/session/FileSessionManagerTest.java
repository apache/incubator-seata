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
package io.seata.server.session;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Resource;

import io.seata.common.XID;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.console.result.PageResult;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.LockStatus;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.console.vo.GlobalSessionVO;
import io.seata.server.storage.file.session.FileSessionManager;
import io.seata.server.store.StoreConfig.SessionMode;
import io.seata.server.util.StoreUtil;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static io.seata.server.session.SessionHolder.CONFIG;
import static io.seata.server.session.SessionHolder.DEFAULT_SESSION_STORE_FILE_DIR;
/**
 * The type File based session manager test.
 *
 * @author tianming.xm @gmail.com
 * @since 2019 /1/22
 */
@SpringBootTest
public class FileSessionManagerTest {


    private static volatile List<SessionManager> sessionManagerList;

    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

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
            for (GlobalSession globalSession : globalSessions) {
                globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
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
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            globalSessionParam.setWithBranch(false);
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), 1).getTime());
            Assertions.assertEquals(0, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), -1).getTime());
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());


            globalSessionParam.setTimeStart(null);
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeEnd(DateUtils.addHours(new Date(), 1).getTime());
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());

            globalSessionParam.setTimeStart(DateUtils.addHours(new Date(), -1).getTime());
            Assertions.assertEquals(3, globalSessionService.query(globalSessionParam).getData().size());
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

}
