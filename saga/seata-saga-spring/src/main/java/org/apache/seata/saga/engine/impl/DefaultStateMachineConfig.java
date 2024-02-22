package org.apache.seata.saga.engine.impl;

import org.apache.seata.saga.engine.config.AbstractStateMachineConfig;
import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.expression.spel.SpringELExpressionFactory;
import org.apache.seata.saga.engine.invoker.ServiceInvokerManager;
import org.apache.seata.saga.engine.invoker.impl.SpringBeanServiceInvoker;
import org.apache.seata.saga.engine.utils.ResourceUtil;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * State machine configuration base spring. In spring context,some uses will be combined with the spring framework.
 * such as expression evaluation add spring el impl, serviceInvoker add spring bean Invoker impl, etc ...
 *
 */
public class DefaultStateMachineConfig extends AbstractStateMachineConfig implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    private String[] resources = new String[] {"classpath*:seata/saga/statelang/**/*.json"};

    @Override
    public void afterPropertiesSet() throws Exception {
        // super init
        super.init();

        // register StateMachine def  after init
        registerStateMachineDef();

        // register spring el ExpressionFactoryManager
        registerSpringElExpressionFactoryManager();

        // register serviceInvoker as spring bean invoker after init
        registerSpringBeanServiceInvoker();
    }

    private void registerStateMachineDef() throws IOException {
        Resource[] registerResources = ResourceUtil.getResources(this.resources);
        InputStream[] resourceAsStreamArray = new InputStream[registerResources.length];
        for (int i = 0; i < registerResources.length; i++) {
            resourceAsStreamArray[i] = registerResources[i].getInputStream();
        }
        getStateMachineRepository().registryByResources(resourceAsStreamArray, getDefaultTenantId());
    }

    private void registerSpringElExpressionFactoryManager() {
        ExpressionFactoryManager expressionFactoryManager = getExpressionFactoryManager();
        SpringELExpressionFactory springELExpressionFactory = new SpringELExpressionFactory(getApplicationContext());
        expressionFactoryManager.putExpressionFactory(ExpressionFactoryManager.DEFAULT_EXPRESSION_TYPE, springELExpressionFactory);
    }

    private void registerSpringBeanServiceInvoker() {
        ServiceInvokerManager manager = getServiceInvokerManager();
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        springBeanServiceInvoker.setSagaJsonParser(getSagaJsonParser());
        springBeanServiceInvoker.setApplicationContext(getApplicationContext());
        springBeanServiceInvoker.setThreadPoolExecutor(getThreadPoolExecutor());
        manager.putServiceInvoker(DomainConstants.SERVICE_TYPE_SPRING_BEAN, springBeanServiceInvoker);
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}