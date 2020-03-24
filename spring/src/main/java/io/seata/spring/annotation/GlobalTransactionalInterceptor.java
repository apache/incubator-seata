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
package io.seata.spring.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.rm.GlobalLockTemplate;
import io.seata.tm.TransactionManagerHolder;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalTemplate;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;


import static io.seata.core.constants.DefaultValues.DEFAULT_CLIENT_SELF_CHECK;
import static io.seata.core.constants.DefaultValues.DEFAULT_CLIENT_SELF_CHECK_ALLOW_TIMES;
import static io.seata.core.constants.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.core.constants.DefaultValues.DEFAULT_CLIENT_SELF_CHECK_PERIOD;

/**
 * The type Global transactional interceptor.
 *
 * @author slievrly
 */
public class GlobalTransactionalInterceptor implements ConfigurationChangeListener, MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptor.class);
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final GlobalLockTemplate<Object> globalLockTemplate = new GlobalLockTemplate<>();
    private final FailureHandler failureHandler;
    private volatile boolean disable;
    private static int selfCheckPeriod;
    private static boolean selfCheck;
    private static int selfCheckAllowTimes;
    private static volatile int autoDemotionNum = 0;
    private static ConcurrentHashMap<String, Integer> demotionMap = new ConcurrentHashMap<>();

    /**
     * initialize selfCheck
     */
    static {
        selfCheck = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.CLIENT_SELF_CHECK,
            DEFAULT_CLIENT_SELF_CHECK);
        if (selfCheck) {
            selfCheckPeriod = ConfigurationFactory.getInstance().getInt(ConfigurationKeys.CLIENT_SELF_CHECK_PERIOD,
                DEFAULT_CLIENT_SELF_CHECK_PERIOD);
            selfCheckAllowTimes = ConfigurationFactory.getInstance()
                .getInt(ConfigurationKeys.CLIENT_SELF_CHECK_ALLOW_TIMES, DEFAULT_CLIENT_SELF_CHECK_ALLOW_TIMES);
            if (selfCheckPeriod > 0 && selfCheckAllowTimes > 0) {
                startSelfCheck();
            }
        }
    }
    /**
     * Instantiates a new Global transactional interceptor.
     *
     * @param failureHandler the failure handler
     */
    public GlobalTransactionalInterceptor(FailureHandler failureHandler) {
        this.failureHandler = failureHandler == null ? DEFAULT_FAIL_HANDLER : failureHandler;
        this.disable =
            ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);
    }

    @Override
    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        Class<?> targetClass =
            methodInvocation.getThis() != null ? AopUtils.getTargetClass(methodInvocation.getThis()) : null;
        Method specificMethod = ClassUtils.getMostSpecificMethod(methodInvocation.getMethod(), targetClass);
        final Method method = BridgeMethodResolver.findBridgedMethod(specificMethod);
        final GlobalTransactional globalTransactionalAnnotation = getAnnotation(method, GlobalTransactional.class);
        String key = null;
        if (null != globalTransactionalAnnotation && globalTransactionalAnnotation.demotion()) {
            StringBuilder builder = new StringBuilder(targetClass.getName()).append(".").append(method.getName());
            key = builder.toString();
            Integer value = demotionMap.get(key);
            if (null != value && value >= globalTransactionalAnnotation.demotionTimes()) {
                LOGGER.warn("this interface has been degraded");
                return methodInvocation.proceed();
            }
        }
        final GlobalLock globalLockAnnotation = getAnnotation(method, GlobalLock.class);
        if (!disable && globalTransactionalAnnotation != null) {
            return handleGlobalTransaction(methodInvocation, globalTransactionalAnnotation, key);
        } else if (!disable && globalLockAnnotation != null) {
            return handleGlobalLock(methodInvocation);
        } else {
            return methodInvocation.proceed();
        }
    }

    private Object handleGlobalLock(final MethodInvocation methodInvocation) throws Exception {
        return globalLockTemplate.execute(() -> {
            try {
                return methodInvocation.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Object handleGlobalTransaction(final MethodInvocation methodInvocation,
        final GlobalTransactional globalTrxAnno, String demotionKey) throws Throwable {
        boolean error = true;
        try {
            Object execute = transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    return methodInvocation.proceed();
                }

                public String name() {
                    String name = globalTrxAnno.name();
                    if (!StringUtils.isNullOrEmpty(name)) {
                        return name;
                    }
                    return formatMethod(methodInvocation.getMethod());
                }

                @Override
                public TransactionInfo getTransactionInfo() {
                    TransactionInfo transactionInfo = new TransactionInfo();
                    transactionInfo.setTimeOut(globalTrxAnno.timeoutMills());
                    transactionInfo.setName(name());
                    transactionInfo.setPropagation(globalTrxAnno.propagation());
                    Set<RollbackRule> rollbackRules = new LinkedHashSet<>();
                    for (Class<?> rbRule : globalTrxAnno.rollbackFor()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (String rbRule : globalTrxAnno.rollbackForClassName()) {
                        rollbackRules.add(new RollbackRule(rbRule));
                    }
                    for (Class<?> rbRule : globalTrxAnno.noRollbackFor()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    for (String rbRule : globalTrxAnno.noRollbackForClassName()) {
                        rollbackRules.add(new NoRollbackRule(rbRule));
                    }
                    transactionInfo.setRollbackRules(rollbackRules);
                    return transactionInfo;
                }
            });
            error = false;
            return execute;
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            switch (code) {
                case RollbackDone:
                    error = false;
                    throw e.getOriginalException();
                case BeginFailure:
                    failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case CommitFailure:
                    failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackFailure:
                    failureHandler.onRollbackFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackRetrying:
                    error = false;
                    failureHandler.onRollbackRetrying(e.getTransaction(), e.getCause());
                    throw e.getCause();
                default:
                    throw new ShouldNeverHappenException(String.format("Unknown TransactionalExecutor.Code: %s", code));
            }
        } finally {
            if (error && !StringUtils.isBlank(demotionKey)) {
                Integer errorNum = demotionMap.get(demotionKey);
                demotionMap.put(demotionKey, null == errorNum ? 1 : ++errorNum);
            }
        }
    }

    private <T extends Annotation> T getAnnotation(Method method, Class<T> clazz) {
        return method == null ? null : method.getAnnotation(clazz);
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
        }
    }

    /**
     * auto upgrade service detection
     */
    private static void startSelfCheck() {
        ScheduledThreadPoolExecutor executor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("SelfCheckWorker", 1, true));
        executor.scheduleAtFixedRate(() -> {
            if (demotionMap.size() > 0) {
                try {
                    TransactionManagerHolder.get()
                        .commit(TransactionManagerHolder.get().begin(null, null, "test", 60000));
                    onSelfCheck(false);
                } catch (Exception e) {
                    onSelfCheck(true);
                }
            }
        }, 10, selfCheckPeriod, TimeUnit.MILLISECONDS);
    }

    private static synchronized void onSelfCheck(boolean isError) {
        if (!isError) {
            autoDemotionNum++;
            if (autoDemotionNum > selfCheckAllowTimes && demotionMap.size() > 0) {
                autoDemotionNum = 0;
                demotionMap.clear();
            }
        }else{
            if (autoDemotionNum > 0) {
                autoDemotionNum--;
            }
        }
    }
}
