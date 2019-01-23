package com.alibaba.fescar.server.session;

import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

/**
 * @author pingchun@meili-inc.com
 * @since 2019/1/22
 */
public class FileBasedSessionManagerTest {

    private SessionManager sessionManager = null;

    @Before
    public void setUp() throws Exception{
        sessionManager = new FileBasedSessionManager("root.data",".");
    }

    @Test
    public void addGlobalSessionTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
    }

    @Test
    public void findGlobalSessionTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        GlobalSession expected =  sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNotNull(expected);
        Assert.assertEquals(expected.getTransactionId(),globalSession.getTransactionId());
        Assert.assertEquals(expected.getApplicationId(),globalSession.getApplicationId());
        Assert.assertEquals(expected.getTransactionServiceGroup(),globalSession.getTransactionServiceGroup());
        Assert.assertEquals(expected.getTransactionName(),globalSession.getTransactionName());
        Assert.assertEquals(expected.getTransactionId(),globalSession.getTransactionId());
        Assert.assertEquals(expected.getStatus(),globalSession.getStatus());
    }

    @Test
    public void updateGlobalSessionStatusTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        globalSession.setStatus(GlobalStatus.Finished);
        sessionManager.updateGlobalSessionStatus(globalSession,GlobalStatus.Finished);
        GlobalSession expected =  sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNotNull(expected);
        Assert.assertEquals(GlobalStatus.Finished,expected.getStatus());
    }

    @Test
    public void removeGlobalSessionTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        sessionManager.removeGlobalSession(globalSession);
        GlobalSession expected =  sessionManager.findGlobalSession(globalSession.getTransactionId());
        Assert.assertNull(expected);

    }

    @Test
    public void addBranchSessionTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.addBranchSession(globalSession,branchSession);
    }

    @Test
    public void updateBranchSessionStatusTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.addBranchSession(globalSession,branchSession);
        sessionManager.updateBranchSessionStatus(branchSession, BranchStatus.PhaseTwo_Committed);
    }

    @Test
    public void removeBranchSessionTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.addBranchSession(globalSession,branchSession);
    }

    @Test
    public void allSessionsTest() throws Exception{
        GlobalSession globalSession1 = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        GlobalSession globalSession2 = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession1);
        sessionManager.addGlobalSession(globalSession2);
        Collection<GlobalSession> globalSessions =  sessionManager.allSessions();
        Assert.assertNotNull(globalSessions);
        Assert.assertEquals(2,globalSessions.size());
    }

    @Test
    public void findGlobalSessionsTest() throws Exception{
        GlobalSession globalSession1 = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        GlobalSession globalSession2 = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.addGlobalSession(globalSession1);
        sessionManager.addGlobalSession(globalSession2);

        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.Begin,30*24*3600);
        List<GlobalSession> globalSessions = sessionManager.findGlobalSessions(sessionCondition);
        Assert.assertNotNull(globalSessions);
        Assert.assertEquals(2,globalSessions.size());
    }

    @Test
    public void onBeginTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
    }

    @Test
    public void onStatusChangeTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        sessionManager.onStatusChange(globalSession,GlobalStatus.Finished);
    }

    @Test
    public void onBranchStatusChangeTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.onAddBranch(globalSession,branchSession);
        sessionManager.onBranchStatusChange(globalSession,branchSession,BranchStatus.PhaseTwo_Committed);
    }

    @Test
    public void onAddBranchTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.onAddBranch(globalSession,branchSession);
    }

    @Test
    public void onRemoveBranchTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationId("demo-child-app");
        branchSession.setTxServiceGroup("my_test_tx_group");
        branchSession.setApplicationData("{\"data\":\"test\"}");
        sessionManager.onAddBranch(globalSession,branchSession);
        sessionManager.onRemoveBranch(globalSession,branchSession);
    }

    @Test
    public void onCloseTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        sessionManager.onClose(globalSession);
    }

    @Test
    public void onEndTest() throws Exception{
        GlobalSession globalSession = new GlobalSession("demo-app","my_test_tx_group","test",6000);
        sessionManager.onBegin(globalSession);
        sessionManager.onEnd(globalSession);
    }
}
