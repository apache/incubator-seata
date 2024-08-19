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
package org.apache.seata.rm.fence;

import org.apache.seata.common.exception.ExceptionUtil;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.common.executor.Callback;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.TccLocalTxActive;
import org.apache.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import org.apache.seata.integration.tx.api.fence.FenceHandler;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceDO;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceStore;
import org.apache.seata.integration.tx.api.fence.store.db.CommonFenceStoreDataBaseDAO;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.fence.interceptor.TccFenceCommitInterceptor;
import org.apache.seata.rm.fence.interceptor.TccFencePrepareInterceptor;
import org.apache.seata.rm.fence.interceptor.TccFenceRollbackInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Common Fence Handler(idempotent, non_rollback, suspend)
 *
 */
public class SpringFenceHandler implements FenceHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringFenceHandler.class);

    protected static final CommonFenceStore COMMON_FENCE_DAO = CommonFenceStoreDataBaseDAO.getInstance();

    protected static DataSource dataSource;

    private static TransactionTemplate transactionTemplate;

    private static final int MAX_THREAD_CLEAN = 1;

    private static final int MAX_QUEUE_SIZE = 500;

    /**
     * limit of delete record by date (per sql)
     */
    private static final int LIMIT_DELETE = 1000;

    private static final LinkedBlockingQueue<FenceLogIdentity> LOG_QUEUE = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    private static FenceLogCleanRunnable fenceLogCleanRunnable;

    private static ExecutorService logCleanExecutor;

    static {
        try {
            initLogCleanExecutor();
            DefaultCommonFenceHandler.get().setFenceHandler(new SpringFenceHandler());
        } catch (Exception e) {
            LOGGER.error("init fence log clean executor error", e);
        }
    }

    public static DataSource getDataSource() {
        return SpringFenceHandler.dataSource;
    }

    public static void setDataSource(DataSource dataSource) {
        SpringFenceHandler.dataSource = dataSource;
    }

    public static void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        SpringFenceHandler.transactionTemplate = transactionTemplate;
    }

    /**
     * common prepare method enhanced
     *
     * @param prepareMethod  prepare method
     * @param targetTCCBean  target common bean
     * @param xid            the global transaction id
     * @param branchId       the branch transaction id
     * @param actionName     the action name
     * @param targetCallback the target callback
     * @return the boolean
     */
    @Override
    public Object prepareFence(Method prepareMethod, Object targetTCCBean, String xid, Long branchId, String actionName, Callback<Object> targetCallback) {
        if (hasTransactionAspectBeforeInterceptor(prepareMethod, targetTCCBean, TccFencePrepareInterceptor.class)) {
            try {
                // The tcc transaction is activated on the service side.
                RootContext.bindTccLocalTxActive(TccLocalTxActive.ACTIVE);
                return targetCallback.execute();
            } catch (Throwable t) {
                throw new SkipCallbackWrapperException(ExceptionUtil.unwrap(t));
            } finally {
                RootContext.unbindTccLocalTxActive();
            }
        } else {
            return transactionTemplate.execute(status -> {
                try {
                    // The tcc transaction is not activated on the service side.
                    RootContext.bindTccLocalTxActive(TccLocalTxActive.UN_ACTIVE);
                    Connection conn = DataSourceUtils.getConnection(dataSource);
                    boolean result = insertCommonFenceLog(conn, xid, branchId, actionName, CommonFenceConstant.STATUS_TRIED);
                    LOGGER.info("Common fence prepare result: {}. xid: {}, branchId: {}", result, xid, branchId);
                    if (result) {
                        return targetCallback.execute();
                    } else {
                        throw new CommonFenceException(String.format("Insert common fence record error, prepare fence failed. xid= %s, branchId= %s", xid, branchId),
                                FrameworkErrorCode.InsertRecordError);
                    }
                } catch (CommonFenceException e) {
                    if (e.getErrcode() == FrameworkErrorCode.DuplicateKeyException) {
                        LOGGER.error("Branch transaction has already rollbacked before,prepare fence failed. xid= {},branchId = {}", xid, branchId);
                        addToLogCleanQueue(xid, branchId);
                    }
                    status.setRollbackOnly();
                    throw new SkipCallbackWrapperException(e);
                } catch (Throwable t) {
                    status.setRollbackOnly();
                    throw new SkipCallbackWrapperException(t);
                } finally {
                    RootContext.unbindTccLocalTxActive();
                }
            });
        }
    }

    /**
     * common commit method enhanced
     *
     * @param commitMethod          commit method
     * @param targetTCCBean         target common bean
     * @param xid                   the global transaction id
     * @param branchId              the branch transaction id
     * @param args                  commit method's parameters
     * @return the boolean
     */
    @Override
    public boolean commitFence(Method commitMethod, Object targetTCCBean,
                               String xid, Long branchId, Object[] args) {
        if (hasTransactionAspectBeforeInterceptor(commitMethod, targetTCCBean, TccFenceCommitInterceptor.class)) {
            try {
                // The tcc transaction is activated on the service side.
                RootContext.bindTccLocalTxActive(TccLocalTxActive.ACTIVE);
                RootContext.bindTccCommitResult(false);
                commitMethod.invoke(targetTCCBean, args);
                Boolean result = RootContext.getTccCommitResult();
                return result;
            } catch (Throwable e) {
                throw new SkipCallbackWrapperException(ExceptionUtil.unwrap(e));
            } finally {
                RootContext.unbindTccLocalTxActive();
                RootContext.unbindTccCommitResult();
            }
        } else {
            return transactionTemplate.execute(status -> {
                try {
                    // The tcc transaction is not activated on the service side.
                    RootContext.bindTccLocalTxActive(TccLocalTxActive.UN_ACTIVE);
                    Connection conn = DataSourceUtils.getConnection(dataSource);
                    CommonFenceDO commonFenceDO = COMMON_FENCE_DAO.queryCommonFenceDO(conn, xid, branchId);
                    if (commonFenceDO == null) {
                        throw new CommonFenceException(String.format("Common fence record not exists, commit fence method failed. xid= %s, branchId= %s", xid, branchId),
                                FrameworkErrorCode.RecordNotExists);
                    }
                    if (CommonFenceConstant.STATUS_COMMITTED == commonFenceDO.getStatus()) {
                        LOGGER.info("Branch transaction has already committed before. idempotency rejected. xid: {}, branchId: {}, status: {}", xid, branchId, commonFenceDO.getStatus());
                        return true;
                    }
                    if (CommonFenceConstant.STATUS_ROLLBACKED == commonFenceDO.getStatus() || CommonFenceConstant.STATUS_SUSPENDED == commonFenceDO.getStatus()) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}, status: {}", xid, branchId, commonFenceDO.getStatus());
                        }
                        return false;
                    }
                    boolean result = updateStatusAndInvokeTargetMethod(conn, commitMethod, targetTCCBean, xid, branchId, CommonFenceConstant.STATUS_COMMITTED, status, args);
                    LOGGER.info("Common fence commit result: {}. xid: {}, branchId: {}", result, xid, branchId);
                    return result;
                } catch (Throwable t) {
                    status.setRollbackOnly();
                    throw new SkipCallbackWrapperException(t);
                } finally {
                    RootContext.unbindTccLocalTxActive();
                }
            });
        }
    }

    /**
     * Common rollback method enhanced
     *
     * @param rollbackMethod        rollback method
     * @param targetTCCBean         target tcc bean
     * @param xid                   the global transaction id
     * @param branchId              the branch transaction id
     * @param args                  rollback method's parameters
     * @param actionName            the action name
     * @return the boolean
     */
    @Override
    public boolean rollbackFence(Method rollbackMethod, Object targetTCCBean,
                                 String xid, Long branchId, Object[] args, String actionName) {
        if (hasTransactionAspectBeforeInterceptor(rollbackMethod, targetTCCBean, TccFenceRollbackInterceptor.class)) {
            try {
                // The tcc transaction is activated on the service side.
                RootContext.bindTccLocalTxActive(TccLocalTxActive.ACTIVE);
                RootContext.bindTccRollbackResult(false);
                rollbackMethod.invoke(targetTCCBean, args);
                Boolean result = RootContext.getTccRollbackResult();
                return result;
            } catch (Exception e) {
                throw new SkipCallbackWrapperException(ExceptionUtil.unwrap(e));
            } finally {
                RootContext.unbindTccLocalTxActive();
                RootContext.unbindTccRollbackResult();
            }
        } else {
            return transactionTemplate.execute(status -> {
                try {
                    // The tcc transaction is not activated on the service side.
                    RootContext.bindTccLocalTxActive(TccLocalTxActive.UN_ACTIVE);
                    Connection conn = DataSourceUtils.getConnection(dataSource);
                    CommonFenceDO commonFenceDO = COMMON_FENCE_DAO.queryCommonFenceDO(conn, xid, branchId);
                    // non_rollback
                    if (commonFenceDO == null) {
                        boolean result = insertCommonFenceLog(conn, xid, branchId, actionName, CommonFenceConstant.STATUS_SUSPENDED);
                        LOGGER.info("Insert common fence record result: {}. xid: {}, branchId: {}", result, xid, branchId);
                        if (!result) {
                            throw new CommonFenceException(String.format("Insert common fence record error, rollback fence method failed. xid= %s, branchId= %s", xid, branchId),
                                    FrameworkErrorCode.InsertRecordError);
                        }
                        return true;
                    } else {
                        if (CommonFenceConstant.STATUS_ROLLBACKED == commonFenceDO.getStatus() || CommonFenceConstant.STATUS_SUSPENDED == commonFenceDO.getStatus()) {
                            LOGGER.info("Branch transaction had already rollbacked before, idempotency rejected. xid: {}, branchId: {}, status: {}", xid, branchId, commonFenceDO.getStatus());
                            return true;
                        }
                        if (CommonFenceConstant.STATUS_COMMITTED == commonFenceDO.getStatus()) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}, status: {}", xid, branchId, commonFenceDO.getStatus());
                            }
                            return false;
                        }
                    }
                    boolean result = updateStatusAndInvokeTargetMethod(conn, rollbackMethod, targetTCCBean, xid, branchId, CommonFenceConstant.STATUS_ROLLBACKED, status, args);
                    LOGGER.info("Common fence rollback result: {}. xid: {}, branchId: {}", result, xid, branchId);
                    return result;
                } catch (Throwable t) {
                    status.setRollbackOnly();
                    Throwable cause = t.getCause();
                    if (cause != null && cause instanceof SQLException) {
                        SQLException sqlException = (SQLException) cause;
                        String sqlState = sqlException.getSQLState();
                        int errorCode = sqlException.getErrorCode();
                        if ("40001".equals(sqlState) && errorCode == 1213) {
                            // MySQL deadlock exception
                            LOGGER.error("Common fence rollback fail. xid: {}, branchId: {}, This exception may be due to the deadlock caused by the transaction isolation level being Repeatable Read. The seata server will try to roll back again, so you can ignore this exception. (To avoid this exception, you can set transaction isolation to Read Committed.)", xid, branchId);
                        }
                    }
                    throw new SkipCallbackWrapperException(t);
                } finally {
                    RootContext.unbindTccLocalTxActive();
                }
            });
        }
    }

    /**
     * Check whether the transaction has been activated
     * @return boolean
     */
    public static boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * Check that the business method's transaction interceptor precedes the target interceptor
     * @param method business method
     * @param proxy proxy
     * @param targetInterceptorClass the target interceptor class
     * @return boolean
     */
    public static boolean hasTransactionAspectBeforeInterceptor(Method method, Object proxy, Class<?> targetInterceptorClass) {
        // Gets the interceptor for the proxy object
        if (proxy instanceof Advised) {
            Advised advised = (Advised) proxy;
            Advisor[] advisors = advised.getAdvisors();

            // Iterate over all interceptors to check if the transaction plane is before the target interceptor
            boolean foundTransactionAspect = false;
            for (Advisor advisor : advisors) {
                if (targetInterceptorClass.isInstance(advisor.getAdvice())) {
                    break;
                }
                if (advisor.getAdvice() instanceof TransactionInterceptor) {
                    // Check if the method is matched by the transaction interceptor's transaction attribute source
                    TransactionAttributeSource attributeSource = ((TransactionInterceptor) advisor.getAdvice()).getTransactionAttributeSource();
                    if (attributeSource != null) {
                        foundTransactionAspect = attributeSource.getTransactionAttribute(method, proxy.getClass()) != null;
                    }
                    break;
                }
            }
            return foundTransactionAspect;
        }
        return false;
    }

    /**
     * Insert Common fence log
     *
     * @param conn     the db connection
     * @param xid      the xid
     * @param branchId the branchId
     * @param status   the status
     * @return the boolean
     */
    protected static boolean insertCommonFenceLog(Connection conn, String xid, Long branchId, String actionName, Integer status) {
        CommonFenceDO commonFenceDO = new CommonFenceDO();
        commonFenceDO.setXid(xid);
        commonFenceDO.setBranchId(branchId);
        commonFenceDO.setActionName(actionName);
        commonFenceDO.setStatus(status);
        return COMMON_FENCE_DAO.insertCommonFenceDO(conn, commonFenceDO);
    }

    /**
     * Update Common Fence status and invoke target method
     *
     * @param method                target method
     * @param targetTCCBean         target bean
     * @param xid                   the global transaction id
     * @param branchId              the branch transaction id
     * @param status                the common fence status
     * @return the boolean
     */
    private static boolean updateStatusAndInvokeTargetMethod(Connection conn, Method method, Object targetTCCBean,
                                                             String xid, Long branchId, int status,
                                                             TransactionStatus transactionStatus,
                                                             Object[] args) throws Throwable {
        boolean result = COMMON_FENCE_DAO.updateCommonFenceDO(conn, xid, branchId, status, CommonFenceConstant.STATUS_TRIED);
        if (result) {
            try {
                // invoke two phase method
                Object ret = method.invoke(targetTCCBean, args);
                if (null != ret) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult) ret).isSuccess();
                    } else {
                        result = (boolean) ret;
                    }
                    // If the business execution result is false, the transaction will be rolled back
                    if (!result) {
                        transactionStatus.setRollbackOnly();
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtil.unwrap(e);
            }
        }
        return result;
    }

    /**
     * Delete Common Fence
     *
     * @param xid      the global transaction id
     * @param branchId the branch transaction id
     * @return the boolean
     */
    public static boolean deleteFence(String xid, Long branchId) {
        return transactionTemplate.execute(status -> {
            boolean ret = false;
            try {
                Connection conn = DataSourceUtils.getConnection(dataSource);
                ret = COMMON_FENCE_DAO.deleteCommonFenceDO(conn, xid, branchId);
            } catch (RuntimeException e) {
                status.setRollbackOnly();
                LOGGER.error("delete fence log failed, xid: {}, branchId: {}", xid, branchId, e);
            }
            return ret;
        });
    }

    /**
     * Delete Common Fence By Datetime
     *
     * @param datetime datetime
     * @return the deleted row count
     */
    @Override
    public int deleteFenceByDate(Date datetime) {
        DataSource dataSource = SpringFenceHandler.getDataSource();
        Connection connection = null;
        int total = 0;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            while (true) {
                Set<String> xidSet = COMMON_FENCE_DAO.queryEndStatusXidsByDate(connection, datetime, LIMIT_DELETE);
                if (xidSet.isEmpty()) {
                    break;
                }
                total += COMMON_FENCE_DAO.deleteTCCFenceDO(connection, new ArrayList<>(xidSet));
                if (xidSet.size() < LIMIT_DELETE) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("delete fence log failed ", e);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        return total;

    }

    private static void initLogCleanExecutor() {
        logCleanExecutor = new ThreadPoolExecutor(MAX_THREAD_CLEAN, MAX_THREAD_CLEAN, Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new NamedThreadFactory("fenceLogCleanThread", MAX_THREAD_CLEAN, true)
        );
        fenceLogCleanRunnable = new FenceLogCleanRunnable();
        logCleanExecutor.submit(fenceLogCleanRunnable);
    }

    protected static void addToLogCleanQueue(final String xid, final long branchId) {
        FenceLogIdentity logIdentity = new FenceLogIdentity();
        logIdentity.setXid(xid);
        logIdentity.setBranchId(branchId);
        try {
            LOG_QUEUE.add(logIdentity);
        } catch (Exception e) {
            LOGGER.warn("Insert tcc fence record into queue for async delete error,xid:{},branchId:{}", xid, branchId, e);
        }
    }

    /**
     * clean fence log that has the final status runnable.
     *
     * @see CommonFenceConstant
     */
    private static class FenceLogCleanRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {

                try {
                    FenceLogIdentity logIdentity = LOG_QUEUE.take();
                    boolean ret = SpringFenceHandler.deleteFence(logIdentity.getXid(), logIdentity.getBranchId());
                    if (!ret) {
                        LOGGER.error("delete fence log failed, xid: {}, branchId: {}", logIdentity.getXid(), logIdentity.getBranchId());
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("take fence log from queue for clean be interrupted", e);
                } catch (Exception e) {
                    LOGGER.error("exception occur when clean fence log", e);
                }
            }
        }
    }

    private static class FenceLogIdentity {
        /**
         * the global transaction id
         */
        private String xid;

        /**
         * the branch transaction id
         */
        private Long branchId;

        public String getXid() {
            return xid;
        }

        public Long getBranchId() {
            return branchId;
        }

        public void setXid(String xid) {
            this.xid = xid;
        }

        public void setBranchId(Long branchId) {
            this.branchId = branchId;
        }
    }
}
