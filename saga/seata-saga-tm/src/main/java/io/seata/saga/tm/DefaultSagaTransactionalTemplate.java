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
package io.seata.saga.tm;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.netty.RmRpcClient;
import io.seata.core.rpc.netty.ShutdownHook;
import io.seata.core.rpc.netty.TmRpcClient;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.saga.rm.SagaResource;
import io.seata.tm.TMClient;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * Template of executing business logic with a global transaction for SAGA mode
 *
 * @author lorne.cl
 */
public class DefaultSagaTransactionalTemplate implements SagaTransactionalTemplate, ApplicationContextAware, DisposableBean, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSagaTransactionalTemplate.class);

    private static final int DEFAULT_TRANS_OPER_TIMEOUT = 60000 * 10;
    private int timeout = DEFAULT_TRANS_OPER_TIMEOUT;

    private String             applicationId;
    private String             txServiceGroup;
    private ApplicationContext applicationContext;

    @Override
    public void commitTransaction(GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeCommit();
            tx.commit();
            triggerAfterCommit();
        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.CommitFailure);
        }
    }

    @Override
    public void rollbackTransaction(GlobalTransaction tx, Throwable ex) throws TransactionException, TransactionalExecutor.ExecutionException {
        triggerBeforeRollback();
        tx.rollback();
        triggerAfterRollback();
        // Successfully rolled back
    }

    @Override
    public GlobalTransaction beginTransaction(TransactionInfo txInfo) throws TransactionalExecutor.ExecutionException {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        try {
            triggerBeforeBegin();
            tx.begin(txInfo.getTimeOut(), txInfo.getName());
            triggerAfterBegin();
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.BeginFailure);

        }
        return tx;
    }

    @Override
    public void reportTransaction(GlobalTransaction tx, GlobalStatus globalStatus) throws TransactionalExecutor.ExecutionException {
        try {
            tx.globalReport(globalStatus);
            triggerAfterCompletion();
        } catch (TransactionException txe) {

            throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.ReportFailure);
        }
    }

    @Override
    public long branchRegister(String resourceId, String clientId, String xid, String applicationData, String lockKeys)
            throws TransactionException {
        return DefaultResourceManager.get().branchRegister(BranchType.SAGA, resourceId, clientId, xid, applicationData, lockKeys);
    }

    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData)
            throws TransactionException{
        DefaultResourceManager.get().branchReport(BranchType.SAGA, xid, branchId, status, applicationData);
    }

    protected void triggerBeforeBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeBegin in hook " + e.getMessage());
            }
        }
    }

    protected void triggerAfterBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterBegin in hook " + e.getMessage());
            }
        }
    }

    protected void triggerBeforeRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeRollback in hook " + e.getMessage());
            }
        }
    }

    protected void triggerAfterRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterRollback in hook " + e.getMessage());
            }
        }
    }

    protected void triggerBeforeCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeCommit in hook " + e.getMessage());
            }
        }
    }

    protected void triggerAfterCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCommit in hook " + e.getMessage());
            }
        }
    }

    @Override
    public void triggerAfterCompletion() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCompletion();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCompletion in hook " + e.getMessage());
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initSeataClient();
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    private void initSeataClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (io.seata.common.util.StringUtils.isNullOrEmpty(applicationId) || io.seata.common.util.StringUtils.isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(
                    "applicationId: " + applicationId + ", txServiceGroup: " + txServiceGroup);
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Transaction Manager Client is initialized. applicationId[" + applicationId + "] txServiceGroup["
                            + txServiceGroup + "]");
        }
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Resource Manager is initialized. applicationId[" + applicationId + "] txServiceGroup[" + txServiceGroup
                            + "]");
        }

        // Only register application as a saga resource
        SagaResource sagaResource = new SagaResource();
        sagaResource.setResourceGroupId(getTxServiceGroup());
        sagaResource.setApplicationId(getApplicationId());
        DefaultResourceManager.get().registerResource(sagaResource);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();

    }

    private void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmRpcClient.getInstance(applicationId, txServiceGroup));
        ShutdownHook.getInstance().addDisposable(RmRpcClient.getInstance(applicationId, txServiceGroup));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void cleanUp() {
        TransactionHookManager.clear();
    }

    protected List<TransactionHook> getCurrentHooks() {
        return TransactionHookManager.getHooks();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }
}