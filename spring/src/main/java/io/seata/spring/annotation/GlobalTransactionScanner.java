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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.seata.common.aot.NativeUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.ReflectionUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.integration.tx.api.util.ProxyUtil;
import io.seata.rm.RMClient;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.spring.annotation.scannercheckers.PackageScannerChecker;
import io.seata.spring.tcc.TccActionInterceptor;
import io.seata.tm.TMClient;
import io.seata.tm.api.FailureHandler;
import io.seata.tm.api.FailureHandlerHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import static io.seata.common.DefaultValues.DEFAULT_DISABLE_GLOBAL_TRANSACTION;
import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP_OLD;

/**
 * The type Global transaction scanner.
 *
 * @author slievrly
 */
public class GlobalTransactionScanner extends AbstractAutoProxyCreator
        implements ConfigurationChangeListener, InitializingBean, ApplicationContextAware, DisposableBean {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    private static final int AT_MODE = 1;
    private static final int MT_MODE = 2;

    private static final int ORDER_NUM = 1024;
    private static final int DEFAULT_MODE = AT_MODE + MT_MODE;

    private static final Set<String> PROXYED_SET = new HashSet<>();
    private static final Set<String> EXCLUDE_BEAN_NAME_SET = new HashSet<>();
    private static final Set<ScannerChecker> SCANNER_CHECKER_SET = new LinkedHashSet<>();

    private static ConfigurableListableBeanFactory beanFactory;

    private MethodInterceptor interceptor;
    private MethodInterceptor globalTransactionalInterceptor;

    private final String applicationId;
    private final String txServiceGroup;
    private final int mode;
    private static String accessKey;
    private static String secretKey;
    private volatile boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, DEFAULT_DISABLE_GLOBAL_TRANSACTION);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final FailureHandler failureHandlerHook;

    private ApplicationContext applicationContext;


    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     */
    public GlobalTransactionScanner(String txServiceGroup) {
        this(txServiceGroup, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String txServiceGroup, int mode) {
        this(txServiceGroup, txServiceGroup, mode);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the default server group
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup) {
        this(applicationId, txServiceGroup, DEFAULT_MODE);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the tx service group
     * @param mode           the mode
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode) {
        this(applicationId, txServiceGroup, mode, null);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, FailureHandler failureHandlerHook) {
        this(applicationId, txServiceGroup, DEFAULT_MODE, failureHandlerHook);
    }

    /**
     * Instantiates a new Global transaction scanner.
     *
     * @param applicationId      the application id
     * @param txServiceGroup     the tx service group
     * @param mode               the mode
     * @param failureHandlerHook the failure handler hook
     */
    public GlobalTransactionScanner(String applicationId, String txServiceGroup, int mode,
                                    FailureHandler failureHandlerHook) {
        setOrder(ORDER_NUM);
        setProxyTargetClass(true);
        this.applicationId = applicationId;
        this.txServiceGroup = txServiceGroup;
        this.mode = mode;
        this.failureHandlerHook = failureHandlerHook;
        FailureHandlerHolder.setFailureHandler(this.failureHandlerHook);
    }

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    public static void setAccessKey(String accessKey) {
        GlobalTransactionScanner.accessKey = accessKey;
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    public static void setSecretKey(String secretKey) {
        GlobalTransactionScanner.secretKey = secretKey;
    }

    @Override
    public void destroy() {
        ShutdownHook.getInstance().destroyAll();
    }

    private void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (DEFAULT_TX_GROUP_OLD.equals(txServiceGroup)) {
            LOGGER.warn("the default value of seata.tx-service-group: {} has already changed to {} since Seata 1.5, " +
                            "please change your default configuration as soon as possible " +
                            "and we don't recommend you to use default tx-service-group's value provided by seata",
                    DEFAULT_TX_GROUP_OLD, DEFAULT_TX_GROUP);
        }
        if (StringUtils.isNullOrEmpty(applicationId) || StringUtils.isNullOrEmpty(txServiceGroup)) {
            throw new IllegalArgumentException(String.format("applicationId: %s, txServiceGroup: %s", applicationId, txServiceGroup));
        }
        //init TM
        TMClient.init(applicationId, txServiceGroup, accessKey, secretKey);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Transaction Manager Client is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Resource Manager is initialized. applicationId[{}] txServiceGroup[{}]", applicationId, txServiceGroup);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
        registerSpringShutdownHook();

    }

    private void registerSpringShutdownHook() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).registerShutdownHook();
            ShutdownHook.removeRuntimeShutdownHook();
        }
        ShutdownHook.getInstance().addDisposable(TmNettyRemotingClient.getInstance(applicationId, txServiceGroup, accessKey, secretKey));
        ShutdownHook.getInstance().addDisposable(RmNettyRemotingClient.getInstance(applicationId, txServiceGroup));
    }

    /**
     * The following will be scanned, and added corresponding interceptor:
     * <p>
     * TM:
     * @see io.seata.spring.annotation.GlobalTransactional // TM annotation
     * Corresponding interceptor:
     * @see io.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler#handleGlobalTransaction(io.seata.integration.tx.api.interceptor.InvocationWrapper, io.seata.integration.tx.api.annotation.AspectTransactional) // TM handler
     * <p>
     * GlobalLock:
     * @see io.seata.spring.annotation.GlobalLock // GlobalLock annotation
     * Corresponding interceptor:
     * @see io.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler#handleGlobalLock(io.seata.integration.tx.api.interceptor.InvocationWrapper, io.seata.spring.annotation.GlobalLock)  // GlobalLock handler
     * <p>
     * TCC mode:
     * @see io.seata.rm.tcc.api.LocalTCC // TCC annotation on interface
     * @see io.seata.rm.tcc.api.TwoPhaseBusinessAction // TCC annotation on try method
     * @see io.seata.integration.tx.api.remoting.RemotingParser // Remote TCC service parser
     * Corresponding interceptor:
     * @see io.seata.rm.tcc.interceptor.TccActionInterceptorHandler // the interceptor of TCC mode
     */
    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // do checkers
        if (!doCheckers(bean, beanName)) {
            return bean;
        }

        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.contains(beanName)) {
                    return bean;
                }
                Object resultBean = ProxyUtil.createProxy(bean);
                if (bean != resultBean) {
                    PROXYED_SET.add(beanName);
                }
                return resultBean;
            }
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    private boolean doCheckers(Object bean, String beanName) {
        if (PROXYED_SET.contains(beanName) || EXCLUDE_BEAN_NAME_SET.contains(beanName)
                || FactoryBean.class.isAssignableFrom(bean.getClass())) {
            return false;
        }

        if (!SCANNER_CHECKER_SET.isEmpty()) {
            for (ScannerChecker checker : SCANNER_CHECKER_SET) {
                try {
                    if (!checker.check(bean, beanName, beanFactory)) {
                        // failed check, do not scan this bean
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Do check failed: beanName={}, checker={}",
                            beanName, checker.getClass().getSimpleName(), e);
                }
            }
        }

        return true;
    }

    private MethodDesc makeMethodDesc(GlobalTransactional anno, Method method) {
        return new MethodDesc(anno, method);
    }

    public static boolean isTccAutoProxy(Class<?> beanClass) {
        Set<Class<?>> interfaceClasses = ReflectionUtil.getInterfaces(beanClass);
        for (Class<?> interClass : interfaceClasses) {
            if (interClass.isAnnotationPresent(LocalTCC.class)) {
                return true;
            }
        }
        return beanClass.isAnnotationPresent(LocalTCC.class);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass, String beanName, TargetSource customTargetSource)
            throws BeansException {
        if (NativeUtils.isSpringAotProcessing()) {
            if (isTccAutoProxy(beanClass)) {
                LOGGER.info("Proxy TCC service: {}", beanName);
                return new Object[]{new TccActionInterceptor()};
            } else if (existsAnnotation(beanClass)) {
                LOGGER.info("Proxy TM bean: {}", beanName);
                return new Object[]{new GlobalTransactionalInterceptor(failureHandlerHook)};
            } else {
                return DO_NOT_PROXY;
            }
        }

        return new Object[]{interceptor};
    }

    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Global transaction is disabled.");
            }
            ConfigurationCache.addConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                    (ConfigurationChangeListener) this);
            return;
        }
        if (initialized.compareAndSet(false, true)) {
            initClient();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.setBeanFactory(applicationContext);
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        if (ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION.equals(event.getDataId())) {
            disableGlobalTransaction = Boolean.parseBoolean(event.getNewValue().trim());
            if (!disableGlobalTransaction && initialized.compareAndSet(false, true)) {
                LOGGER.info("{} config changed, old value:true, new value:{}", ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION,
                        event.getNewValue());
                initClient();
                ConfigurationCache.removeConfigListener(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, this);
            }
        }
    }

    public static void setBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        GlobalTransactionScanner.beanFactory = beanFactory;
    }

    public static void addScannablePackages(String... packages) {
        PackageScannerChecker.addScannablePackages(packages);
    }

    public static void addScannerCheckers(Collection<ScannerChecker> scannerCheckers) {
        if (CollectionUtils.isNotEmpty(scannerCheckers)) {
            scannerCheckers.remove(null);
            SCANNER_CHECKER_SET.addAll(scannerCheckers);
        }
    }

    public static void addScannerCheckers(ScannerChecker... scannerCheckers) {
        if (ArrayUtils.isNotEmpty(scannerCheckers)) {
            addScannerCheckers(Arrays.asList(scannerCheckers));
        }
    }

    public static void addScannerExcludeBeanNames(String... beanNames) {
        if (ArrayUtils.isNotEmpty(beanNames)) {
            EXCLUDE_BEAN_NAME_SET.addAll(Arrays.asList(beanNames));
        }
    }
}
