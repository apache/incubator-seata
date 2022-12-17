package io.seata.commonapi.interceptor.handler;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.commonapi.annotation.AspectTransactional;
import io.seata.commonapi.event.DegradeCheckEvent;
import io.seata.commonapi.interceptor.InvocationWrapper;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.event.EventBus;
import io.seata.core.event.GuavaEventBus;
import io.seata.core.model.GlobalLockConfig;
import io.seata.rm.GlobalLockExecutor;
import io.seata.rm.GlobalLockTemplate;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.DefaultFailureHandlerImpl;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.TransactionalExecutor;
import io.seata.tm.api.TransactionalTemplate;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.BridgeMethodResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.common.DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class GlobalTransactionalInterceptorHandler extends AbstractProxyInvocationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionalInterceptorHandler.class);
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private final TransactionalTemplate transactionalTemplate = new TransactionalTemplate();
    private final GlobalLockTemplate globalLockTemplate = new GlobalLockTemplate();

    private Class[] interfaceToProxy;
    private Set<String> methodsToProxy;

    private volatile boolean disable;
    private static final AtomicBoolean ATOMIC_DEGRADE_CHECK = new AtomicBoolean(false);
    private static volatile Integer degradeNum = 0;
    private static volatile Integer reachNum = 0;
    private static int degradeCheckAllowTimes;
    protected AspectTransactional aspectTransactional;
    private static int defaultGlobalTransactionTimeout = 0;

    private final FailureHandler failureHandler;

    private static final EventBus EVENT_BUS = new GuavaEventBus("degradeCheckEventBus", true);


    public GlobalTransactionalInterceptorHandler(FailureHandler failureHandler, Class[] interfaceToProxy, Set<String> methodsToProxy) {
        this.failureHandler = failureHandler == null ? DEFAULT_FAIL_HANDLER : failureHandler;
        this.interfaceToProxy = interfaceToProxy;
        this.methodsToProxy = methodsToProxy;
        this.disable = ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                DEFAULT_DISABLE_GLOBAL_TRANSACTION);
    }

    @Override
    public Class[] getInterfaceToProxy() {
        return interfaceToProxy;
    }

    @Override
    public Set<String> getMethodsToProxy() {
        return methodsToProxy;
    }

    @Override
    public boolean interfaceProxyMode() {
        return false;
    }

    @Override
    protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
        Class targetClass = invocation.getTarget().getClass();
        Method specificMethod = invocation.getMethod();
        if (specificMethod != null && !specificMethod.getDeclaringClass().equals(Object.class)) {
            final Method method = BridgeMethodResolver.findBridgedMethod(specificMethod);
            final GlobalTransactional globalTransactionalAnnotation = getAnnotation(method, targetClass, GlobalTransactional.class);
            final GlobalLock globalLockAnnotation = getAnnotation(method, targetClass, GlobalLock.class);
            boolean localDisable = disable || (ATOMIC_DEGRADE_CHECK.get() && degradeNum >= degradeCheckAllowTimes);
            if (!localDisable) {
                if (globalTransactionalAnnotation != null || this.aspectTransactional != null) {
                    AspectTransactional transactional;
                    if (globalTransactionalAnnotation != null) {
                        transactional = new AspectTransactional(globalTransactionalAnnotation.timeoutMills(),
                                globalTransactionalAnnotation.name(), globalTransactionalAnnotation.rollbackFor(),
                                globalTransactionalAnnotation.rollbackForClassName(),
                                globalTransactionalAnnotation.noRollbackFor(),
                                globalTransactionalAnnotation.noRollbackForClassName(),
                                globalTransactionalAnnotation.propagation(),
                                globalTransactionalAnnotation.lockRetryInterval(),
                                globalTransactionalAnnotation.lockRetryTimes());
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


    private Object handleGlobalLock(final InvocationWrapper methodInvocation, final GlobalLock globalLockAnno) throws Throwable {
        return globalLockTemplate.execute(new GlobalLockExecutor() {
            @Override
            public Object execute() throws Throwable {
                return methodInvocation.proceed();
            }

            @Override
            public GlobalLockConfig getGlobalLockConfig() {
                GlobalLockConfig config = new GlobalLockConfig();
                config.setLockRetryInterval(globalLockAnno.lockRetryInterval());
                config.setLockRetryTimes(globalLockAnno.lockRetryTimes());
                return config;
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
            TransactionalExecutor.Code code = e.getCode();
            switch (code) {
                case RollbackDone:
                    throw e.getOriginalException();
                case BeginFailure:
                    succeed = false;
                    failureHandler.onBeginFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case CommitFailure:
                    succeed = false;
                    failureHandler.onCommitFailure(e.getTransaction(), e.getCause());
                    throw e.getCause();
                case RollbackFailure:
                    failureHandler.onRollbackFailure(e.getTransaction(), e.getOriginalException());
                    throw e.getOriginalException();
                case RollbackRetrying:
                    failureHandler.onRollbackRetrying(e.getTransaction(), e.getOriginalException());
                    throw e.getOriginalException();
                case TimeoutRollback:
                    failureHandler.onTimeoutRollback(e.getTransaction(), e.getOriginalException());
                    throw e.getCause();
                default:
                    throw new ShouldNeverHappenException(String.format("Unknown TransactionalExecutor.Code: %s", code));
            }
        } finally {
            if (ATOMIC_DEGRADE_CHECK.get()) {
                EVENT_BUS.post(new DegradeCheckEvent(succeed));
            }
        }
    }


    public <T extends Annotation> T getAnnotation(Method method, Class<?> targetClass, Class<T> annotationClass) {
        return Optional.ofNullable(method).map(m -> m.getAnnotation(annotationClass))
                .orElse(Optional.ofNullable(targetClass).map(t -> t.getAnnotation(annotationClass)).orElse(null));
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
}
