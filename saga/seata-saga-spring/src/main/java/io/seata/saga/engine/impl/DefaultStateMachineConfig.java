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
package io.seata.saga.engine.impl;

import io.seata.saga.engine.config.AbstractStateMachineConfig;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.expression.spel.SpringELExpressionFactory;
import io.seata.saga.engine.invoker.ServiceInvokerManager;
import io.seata.saga.engine.invoker.impl.SpringBeanServiceInvoker;
import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.util.ResourceUtil;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Default state machine configuration
 *
 * @author lorne.cl
 */
public class DefaultStateMachineConfig extends AbstractStateMachineConfig implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStateMachineConfig.class);

    private ApplicationContext applicationContext;

    private String[] resources = new String[] {"classpath*:seata/saga/statelang/**/*.json"};

    @Override
    public void afterPropertiesSet() throws Exception {
        // load resource as spring classPathResource
        try {
            Resource[] registerResources = ResourceUtil.getResources(this.resources);
            InputStream[] resourceAsStreamArray = new InputStream[registerResources.length];
            for (int i = 0; i < registerResources.length; i++) {
                resourceAsStreamArray[i] = registerResources[i].getInputStream();
            }
            setStateMachineDefInputStreamArray(resourceAsStreamArray);
        } catch (IOException e) {
            LOGGER.error("Load State Language Resources failed.", e);
        }

        // super init
        super.init();

        // register spring el ExpressionFactoryManager
        registerSpringElExpressionFactoryManager();

        // register serviceInvoker as spring bean invoker after init
        registerSpringBeanServiceInvoker();
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

    @Override
    public StateLangStore initStateLogStoreStore() throws Exception {
        return null;
    }

    @Override
    public StateLogStore initStateLogStore() throws Exception {
        return null;
    }
}