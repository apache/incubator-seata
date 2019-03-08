/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.spring.annotation;

import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.rm.RMClient;
import com.alibaba.fescar.rm.tcc.api.TwoPhaseBusinessAction;
import com.alibaba.fescar.rm.tcc.remoting.Protocols;
import com.alibaba.fescar.rm.tcc.remoting.RemotingDesc;
import com.alibaba.fescar.rm.tcc.remoting.parser.DefaultRemotingParser;
import com.alibaba.fescar.spring.tcc.TccActionInterceptor;
import com.alibaba.fescar.spring.util.SpringProxyUtils;
import com.alibaba.fescar.tm.TMClient;
import com.alibaba.fescar.tm.api.DefaultFailureHandlerImpl;
import com.alibaba.fescar.tm.api.FailureHandler;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * The type Global transaction scanner.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /12/28 17:23
 * @FileName: GlobalTransactionScanner
 * @Description:
 */
public class GlobalTransactionScanner extends AbstractAutoProxyCreator implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTransactionScanner.class);

    private static final int AT_MODE = 1;
    private static final int MT_MODE = 2;

    private static final int ORDER_NUM = 1024;
    private static final int DEFAULT_MODE = AT_MODE + MT_MODE;

    private static final Set<String> PROXYED_SET = new HashSet<>();
    private static final FailureHandler DEFAULT_FAIL_HANDLER = new DefaultFailureHandlerImpl();

    private MethodInterceptor interceptor;

    private final String applicationId;
    private final String txServiceGroup;
    private final int mode;
    private final boolean disableGlobalTransaction =
        ConfigurationFactory.getInstance().getBoolean("service.disableGlobalTransaction", false);

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
        this(applicationId, txServiceGroup, mode, DEFAULT_FAIL_HANDLER);
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
    }

    private void initClient() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Initializing Global Transaction Clients ... ");
        }
        if (StringUtils.isEmpty(applicationId) || StringUtils.isEmpty(txServiceGroup)) {
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
            LOGGER.info("Resource Manager is initialized. applicationId[" + applicationId  + "] txServiceGroup["  + txServiceGroup + "]");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Global Transaction Clients are initialized. ");
        }
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (disableGlobalTransaction) {
            return bean;
        }
        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.contains(beanName)) {
                    return bean;
                }
                interceptor = null;
                //是否 TCC 动态代理
                if(isTccAutoProxy(bean, beanName)){
                    //TCC 动态代理，代理 proxy bean of sofa:reference/dubbo:reference , and LocalTCC
                    RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
                    interceptor = new TccActionInterceptor(remotingDesc);
                }else {
                    //@GlobalTransactional 动态代理
                    Class<?> serviceInterface = SpringProxyUtils.findTargetClass(bean);
                    Method[] methods = serviceInterface.getMethods();
                    LinkedList<MethodDesc> methodDescList = new LinkedList<>();
                    for (Method method : methods) {
                        GlobalTransactional anno = method.getAnnotation(GlobalTransactional.class);
                        if (anno != null) {
                            methodDescList.add(makeMethodDesc(anno, method));
                        }
                    }
                    if (methodDescList.isEmpty()) {
                        return bean;
                    }
                    if (interceptor == null) {
                        interceptor = new GlobalTransactionalInterceptor(failureHandlerHook);
                    }
                }
                LOGGER.info("Bean["+ bean.getClass().getName() +"] with name ["+beanName+"] would use interceptor [" + interceptor.getClass().getName()  + "]");
                if (!AopUtils.isAopProxy(bean)) {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = SpringProxyUtils.getAdvisedSupport(bean);
                    Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                    for (Advisor avr : advisor) {
                        advised.addAdvisor(0, avr);
                    }
                }
                PROXYED_SET.add(beanName);
                return bean;
            }
        } catch (Exception exx) {
            throw new RuntimeException(exx);
        }
    }

    /**
     * is auto proxy TCC bean
     * @param bean
     * @param beanName
     * @return
     */
    protected boolean isTccAutoProxy(Object bean, String beanName){
        RemotingDesc remotingDesc = null;
        //解析remoting bean 信息
        boolean isRemotingBean = parserRemotingServiceInfo(bean, beanName);
        //是否是远程服务bean
        if(isRemotingBean) {
            remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
            if(remotingDesc != null && remotingDesc.getProtocol() == Protocols.IN_JVM.getCode()){
                //LocalTCC
                return isTccProxyTargetBean(remotingDesc);
            }else {
                // sofa:reference / dubbo:reference, factory bean 不代理
                return false;
            }
        }else{
            //获取解析结果
            remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
            if(remotingDesc == null){
                //FactoryBean 判断
                if(isRemotingFactoryBean(bean, beanName)){
                    remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
                    //判断是否需要代理
                    return isTccProxyTargetBean(remotingDesc);
                }else {
                    return false;
                }
            }else {
                //判断是否需要代理
                return isTccProxyTargetBean(remotingDesc);
            }
        }
    }

    /**
     * 当前bean是proxy，判断其FactoryBean是否是 Remoting bean
     *
     * @return
     */
    protected boolean isRemotingFactoryBean(Object bean, String beanName) {
        if(!SpringProxyUtils.isProxy(bean)){
            return false;
        }
        //the FactoryBean of proxy bean，以防 bean 扫描遗漏
        String factoryBeanName = new StringBuilder().append("&").append(beanName).toString();
        Object factoryBean = null;
        if(applicationContext != null && applicationContext.containsBean(factoryBeanName)){
            factoryBean = applicationContext.getBean(factoryBeanName);
        }
        //无factory bean，无需动态代理
        if(factoryBean == null ){
            return false;
        }
        //解析factory bean信息
        return parserRemotingServiceInfo(factoryBean, beanName);
    }

    /**
     * 是否是TCC代理的目标bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     * @param remotingDesc
     * @return
     */
    protected boolean isTccProxyTargetBean(RemotingDesc remotingDesc){
        if(remotingDesc == null) {
            return false;
        }
        //check if it is TCC bean
        boolean isTccClazz = false;
        Class<?> tccInterfaceClazz = remotingDesc.getInterfaceClass();
        Method[] methods = tccInterfaceClazz.getMethods();
        TwoPhaseBusinessAction twoPhaseBusinessAction = null;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if(twoPhaseBusinessAction != null ){
                isTccClazz = true;
                break;
            }
        }
        if(!isTccClazz){
            return false;
        }
        Protocols protocols = Protocols.valueOf(remotingDesc.getProtocol());
        //LocalTCC
        if(Protocols.IN_JVM.equals(protocols)){
            //in jvm TCC bean， 创建 AOP 切面
            return true;
        }
        // sofa:reference /  dubbo:reference, 需要创建AOP切面
        if(remotingDesc.isReference()){
            return true;
        }
        return false;
    }

    /**
     * 解析 remoting bean 信息，提取 sofa:service、sofa:reference、dubbo:reference、dubbo:service 信息
     * @param bean
     * @param beanName
     * @return 是 sofa:service、sofa:reference、dubbo:reference、dubbo:service 返回true，否则返回false
     */
    protected boolean parserRemotingServiceInfo(Object bean, String beanName) {
        //是否是远程服务bean
        if(DefaultRemotingParser.get().isRemoting(bean, beanName)) {
            //解析remoting bean 信息
            DefaultRemotingParser.get().parserRemotingServiceInfo(bean, beanName);
            return true;
        }
        return false;
    }

    private MethodDesc makeMethodDesc(GlobalTransactional anno, Method method) {
        return new MethodDesc(anno, method);
    }


    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass, String beanName, TargetSource customTargetSource)
        throws BeansException {
        return new Object[] {interceptor};
    }


    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Global transaction is disabled.");
            }
            return;
        }
        initClient();

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
