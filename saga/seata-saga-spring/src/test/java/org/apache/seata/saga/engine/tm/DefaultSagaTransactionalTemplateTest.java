package org.apache.seata.saga.engine.tm;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class DefaultSagaTransactionalTemplateTest {
    private static SagaTransactionalTemplate sagaTransactionalTemplate;

    @BeforeEach
    public void init() {
        sagaTransactionalTemplate = new DefaultSagaTransactionalTemplate();
    }

    @Test
    public void testCommitTransaction() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.commitTransaction(mockGlobalTransaction));
    }

    @Test
    public void testRollbackTransaction() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.rollbackTransaction(mockGlobalTransaction, null));
    }

    @Test
    public void testBeginTransaction() {
        TransactionInfo transactionInfo = new TransactionInfo();
        Assertions.assertThrows(NoClassDefFoundError.class,
                () -> sagaTransactionalTemplate.beginTransaction(transactionInfo));
    }

    @Test
    public void testReloadTransaction() {
        Assertions.assertThrows(NoClassDefFoundError.class,
                () -> sagaTransactionalTemplate.reloadTransaction(""));
    }

    @Test
    public void testReportTransaction() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        GlobalStatus globalStatus = GlobalStatus.Committed;
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.reportTransaction(mockGlobalTransaction, globalStatus));
    }

    @Test
    public void testBranchRegister() {
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA, resourceManager);
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.branchRegister("",
                "", "", "", ""));
    }

    @Test
    public void testBranchReport() {
        ResourceManager resourceManager = Mockito.mock(ResourceManager.class);
        Mockito.doNothing().when(resourceManager).registerResource(any(Resource.class));
        DefaultResourceManager.get();
        DefaultResourceManager.mockResourceManager(BranchType.SAGA, resourceManager);
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.branchReport("",
                0, BranchStatus.Unknown, ""));
    }

    @Test
    public void testTriggerAfterCompletion() {
        MockGlobalTransaction mockGlobalTransaction = new MockGlobalTransaction();
        Assertions.assertDoesNotThrow(() -> sagaTransactionalTemplate.triggerAfterCompletion(mockGlobalTransaction));
    }
}