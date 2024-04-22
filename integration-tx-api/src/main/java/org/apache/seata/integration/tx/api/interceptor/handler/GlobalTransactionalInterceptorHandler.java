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
package org.apache.seata.integration.tx.api.interceptor.handler;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.Subscribe;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.event.EventBus;
import org.apache.seata.core.event.GuavaEventBus;
import org.apache.seata.core.exception.TmTransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.integration.tx.api.annotation.AspectTransactional;
import org.apache.seata.integration.tx.api.event.DegradeCheckEvent;
import org.apache.seata.integration.tx.api.interceptor.InvocationHandlerType;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.util.ClassUtils;
import org.apache.seata.rm.GlobalLockExecutor;
import org.apache.seata.rm.GlobalLockTemplate;
import org.apache.seata.spring.annotation.GlobalLock;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.FailureHandler;
import org.apache.seata.tm.api.FailureHandlerHolder;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.TransactionalExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.apache.seata.tm.api.transaction.NoRollbackRule;
import org.apache.seata.tm.api.transaction.RollbackRule;
import org.apache.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static org.apache.seata.common.DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_PERIOD;
import static org.apache.seata.tm.api.GlobalTransactionRole.Participant;

/**
 * The type Global transactional interceptor handler.
 *
 */
public class GlobalTransactionalInterceptorHandler extends AbstractProxyInvocationHandler implements CachedConfigurationChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptorHandler.class);

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final GlobalLockTemplate globalLockTemplate = new GlobalLockTemplate();

    private Set<String> methodsToProxy;

    private volatile boolean disable;
    private static final AtomicBoolean ATOMIC_DEGRADE_CHECK = new AtomicBoolean(false);
    private static volatile Integer degradeNum = 0;
    private static volatile Integer reachNum = 0;
    private static int degradeCheckAllowTimes;
    protected AspectTransactional aspectTransactional;
    private static int degradeCheckPeriod;

    private static int defaultGlobalTransactionTimeout = 0;

    private final FailureHandler failureHandler;

    private static final EventBus EVENT_BUS = new GuavaEventBus("degradeCheckEventBus", true);
    private static volatile ScheduledThreadPoolExecutor executor;

    private void initDefaultGlobalTransactionTimeout() {
        if (GlobalTransactionalInterceptorHandler.defaultGlobalTransactionTimeout <= 0) {
            int defaultGlobalTransactionTimeout;
            try {
                defaultGlobalTransactionTimeout = ConfigurationFactory.getInstance().getInt(
                        ConfigurationKeys.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT, DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);
            } catch (Exception e) {
                LOGGER.error("Illegal global transaction timeout value: " + e.getMessage());
                defaultGlobalTransactionTimeout = DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
            }
            if (defaultGlobalTransactionTimeout <= 0) {
                LOGGER.warn("Global transaction timeout value '{}' is illegal, and has been reset to the default value '{}'",
                        defaultGlobalTransactionTimeout, DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);
                defaultGlobalTransactionTimeout = DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
            }
            GlobalTransactionalInterceptorHandler.defaultGlobalTransactionTimeout = defaultGlobalTransactionTimeout;
        }
    }

    public GlobalTransactionalInterceptorHandler(FailureHandler failureHandler, Set<String> methodsToProxy) {
        this.failureHandler = failureHandler == null ? FailureHandlerHolder.getFailureHandler() : failureHandler;
        this.methodsToProxy = methodsToProxy;
        Configuration configuration = ConfigurationFactory.getInstance();
        this.disable = configuration.getBoolean(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                DEFAULT_DISABLE_GLOBAL_TRANSACTION);
        boolean degradeCheck = configuration.getBoolean(ConfigurationKeys.CLIENT_DEGRADE_CHECK,
                DEFAULT_TM_DEGRADE_CHECK);
        degradeCheckPeriod = configuration.getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_PERIOD, DEFAULT_TM_DEGRADE_CHECK_PERIOD);
        degradeCheckAllowTimes = configuration.getInt(ConfigurationKeys.CLIENT_DEGRADE_CHECK_ALLOW_TIMES, DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES);
        EVENT_BUS.register(this);
        if (degradeCheck && degradeCheckPeriod > 0 && degradeCheckAllowTimes > 0) {
            startDegradeCheck();
        }
        configuration.addConfigListener(ConfigurationKeys.CLIENT_DEGRADE_CHECK, this);
        this.initDefaultGlobalTransactionTimeout();
    }

    public GlobalTransactionalInterceptorHandler(FailureHandler failureHandler, Set<String> methodsToProxy, AspectTransactional aspectTransactional) {
        this(failureHandler, methodsToProxy);
        this.aspectTransactional = aspectTransactional;
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        Class<?> targetClass = invocation.getTarget().getClass();
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        if (specificMethod != null && !specificMethod.getDeclaringClass().equals(Object.class)) {
            boolean localDisable = disable || (ATOMIC_DEGRADE_CHECK.get() && degradeNum >= degradeCheckAllowTimes);
            if (!localDisable) {
                final AspectTransactional globalTransactionalAnnotation = getAspectTransactional(specificMethod, targetClass);
                final GlobalLockConfig globalLockAnnotation = getGlobalLockConfig(specificMethod, targetClass);
                if (globalTransactionalAnnotation != null || this.aspectTransactional != null) {
                    AspectTransactional transactional;
                    if (globalTransactionalAnnotation != null) {
                        transactional = globalTransactionalAnnotation;
                    } else {
                        transactional = this.aspectTransactional;
                    }
                    return handleGlobalTransaction(invocation, transactional);
                } else if (globalLockAnnotation != null) {
                    return handleGlobalLock(invocation, globalLockAnnotation);
                }
            }
        }
        return invocation.proceed();
    }


    private Object handleGlobalLock(final InvocationWrapper methodInvocation, final GlobalLockConfig globalLockConfig) throws Throwable {
        return globalLockTemplate.execute(new GlobalLockExecutor() {
            @Override
            public Object execute() throws Throwable {
                return methodInvocation.proceed();
            }

            @Override
            public GlobalLockConfig getGlobalLockConfig() {
                return globalLockConfig;
            }
        });
    }

    Object handleGlobalTransaction(final InvocationWrapper methodInvocation,
                                   final AspectTransactional aspectTransactional) throws Throwable {
        boolean succeed = true;
        try {
            return transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    return methodInvocation.proceed();
                }

                public String name() {
                    String name = aspectTransactional.getName();
                    if (!StringUtils.isNullOrEmpty(name)) {
                        return name;
                    }
                    return formatMethod(methodInvocation.getMethod());
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    // reset the value of timeout
                    int timeout = aspectTransactional.getTimeoutMills();
                    if (timeout <= 0 || timeout == DEFAULT_GLOBAL_TRANSACTION_TIMEOUT) {
                        timeout = defaultGlobalTransactionTimeout;
                    }

                    TransactionInfo transactionInfo = new TransactionInfo();
                    transactionInfo.setTimeOut(timeout);
                    transactionInfo.setName(name());
                    transactionInfo.setPropagation(aspectTransactional.getPropagation());
                    transactionInfo.setLockRetryInterval(aspectTransactional.getLockRetryInterval());
                    transactionInfo.setLockRetryTimes(aspectTransactional.getLockRetryTimes());
                    transactionInfo.setLockStrategyMode(aspectTransactional.getLockStrategyMode());
                    Set<RollbackRule> rollbackRules = new LinkedHashSet<>();
                    for (Class<?> rbRule : aspectTransactional.getRollbackFor()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (String rbRule : aspectTransactional.getRollbackForClassName()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (Class<?> rbRule : aspectTransactional.getNoRollbackFor()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    for (String rbRule : aspectTransactional.getNoRollbackForClassName()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    transactionInfo.setRollbackRules(rollbackRules);
                    return transactionInfo;
                }
            });
        } catch (TransactionalExecutor.ExecutionException e) {
            GlobalTransaction globalTransaction = e.getTransaction();

            // If Participant, just throw the exception to original.
            if (globalTransaction.getGlobalTransactionRole() == Participant) {
                throw e.getOriginalException();
            }

            TransactionalExecutor.Code code = e.getCode();
            Throwable cause = e.getCause();
            boolean timeout = isTimeoutException(cause);
            switch (code) {
                case RollbackDone:
                    if (timeout) {
                        throw cause;
                    } else {
                        throw e.getOriginalException();
                    }
                case BeginFailure:
                    succeed = false;
                    failureHandler.onBeginFailure(globalTransaction, cause);
                    throw cause;
                case CommitFailure:
                    succeed = false;
                    failureHandler.onCommitFailure(globalTransaction, cause);
                    throw cause;
                case RollbackFailure:
                    failureHandler.onRollbackFailure(globalTransaction, e.getOriginalException());
                    throw e.getOriginalException();
                case Rollbacking:
                    failureHandler.onRollbacking(globalTransaction, e.getOriginalException());
                    if (timeout) {
                        throw cause;
                    } else {
                        throw e.getOriginalException();
                    }
                default:
                    throw new ShouldNeverHappenException(String.format("Unknown TransactionalExecutor.Code: %s", code), e.getOriginalException());
            }
        } finally {
            if (ATOMIC_DEGRADE_CHECK.get()) {
                EVENT_BUS.post(new DegradeCheckEvent(succeed));
            }
        }
    }

    public GlobalLockConfig getGlobalLockConfig(Method method, Class<?> targetClass) {
        final GlobalLock globalLockAnno = getAnnotation(method, targetClass, GlobalLock.class);
        if (globalLockAnno != null) {
            GlobalLockConfig config = new GlobalLockConfig();
            config.setLockRetryInterval(globalLockAnno.lockRetryInterval());
            config.setLockRetryTimes(globalLockAnno.lockRetryTimes());
            return config;
        } else {
            return null;
        }
    }

    public AspectTransactional getAspectTransactional(Method method, Class<?> targetClass) {
        final GlobalTransactional globalTransactionalAnnotation =
            getAnnotation(method, targetClass, GlobalTransactional.class);
        return globalTransactionalAnnotation != null ?
            new AspectTransactional(globalTransactionalAnnotation.timeoutMills(), globalTransactionalAnnotation.name(),
                globalTransactionalAnnotation.rollbackFor(), globalTransactionalAnnotation.rollbackForClassName(),
                globalTransactionalAnnotation.noRollbackFor(), globalTransactionalAnnotation.noRollbackForClassName(),
                globalTransactionalAnnotation.propagation(), globalTransactionalAnnotation.lockRetryInterval(),
                globalTransactionalAnnotation.lockRetryTimes(), globalTransactionalAnnotation.lockStrategyMode()) :
            null;
    }

    private String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        int in = 0;
        for (Class<?> clazz : params) {
            sb.append(clazz.getName());
            if (++in < params.length) {
                sb.append(", ");
            }
        }
        return sb.append(")").toString();
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            LOGGER.info("{} config changed, old value:{}, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                    disable, event.getNewValue());
            disable = Boolean.parseBoolean(event.getNewValue().trim());
        } else if (ConfigurationKeys.CLIENT_DEGRADE_CHECK.equals(event.getDataId())) {
            boolean degradeCheck = Boolean.parseBoolean(event.getNewValue());
            if (!degradeCheck) {
                degradeNum = 0;
                stopDegradeCheck();
            } else if (degradeCheckPeriod > 0 && degradeCheckAllowTimes > 0) {
                startDegradeCheck();
            }
        }
    }

    /**
     * stop auto degrade
     */
    private static void stopDegradeCheck() {
        if (!ATOMIC_DEGRADE_CHECK.compareAndSet(true, false)) {
            return;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * auto upgrade service detection
     */
    private static void startDegradeCheck() {
        if (!ATOMIC_DEGRADE_CHECK.compareAndSet(false, true)) {
            return;
        }
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("degradeCheckWorker", 1, true));
        executor.scheduleAtFixedRate(() -> {
            if (ATOMIC_DEGRADE_CHECK.get()) {
                try {
                    String xid = TransactionManagerHolder.get().begin(null, null, "degradeCheck", 60000);
                    TransactionManagerHolder.get().commit(xid);
                    EVENT_BUS.post(new DegradeCheckEvent(true));
                } catch (Exception e) {
                    EVENT_BUS.post(new DegradeCheckEvent(false));
                }
            }
        }, degradeCheckPeriod, degradeCheckPeriod, TimeUnit.MILLISECONDS);
    }
    @Subscribe
    public static void onDegradeCheck(DegradeCheckEvent event) {
        if (event.isRequestSuccess()) {
            if (degradeNum >= degradeCheckAllowTimes) {
                reachNum++;
                if (reachNum >= degradeCheckAllowTimes) {
                    reachNum = 0;
                    degradeNum = 0;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("the current global transaction has been restored");
                    }
                }
            } else if (degradeNum != 0) {
                degradeNum = 0;
            }
        } else {
            if (degradeNum < degradeCheckAllowTimes) {
                degradeNum++;
                if (degradeNum >= degradeCheckAllowTimes) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("the current global transaction has been automatically downgraded");
                    }
                }
            } else if (reachNum != 0) {
                reachNum = 0;
            }
        }
    }

    private boolean isTimeoutException(Throwable th) {
        if (null == th) {
            return false;
        }
        if (th instanceof TmTransactionException) {
            TmTransactionException exx = (TmTransactionException)th;
            if (TransactionExceptionCode.TransactionTimeout == exx.getCode()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
    }

    @Override
    public SeataInterceptorPosition getPosition() {
        return SeataInterceptorPosition.BeforeTransaction;
    }


    @Override
    public String type() {
        return InvocationHandlerType.GlobalTransactional.name();
    }
}
