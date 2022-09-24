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
package io.seata.spring.boot.autoconfigure;

import io.seata.common.holder.ObjectHolder;
import io.seata.commonapi.fence.config.CommonFenceConfig;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.spring.boot.autoconfigure.properties.SagaAsyncThreadPoolProperties;
import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.client.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.*;

/**
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class SeataClientEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        PROPERTY_BEAN_MAP.put(SEATA_PREFIX, SeataProperties.class);

        PROPERTY_BEAN_MAP.put(CLIENT_RM_PREFIX, RmProperties.class);
        PROPERTY_BEAN_MAP.put(CLIENT_TM_PREFIX, TmProperties.class);
        PROPERTY_BEAN_MAP.put(LOCK_PREFIX, LockProperties.class);
        PROPERTY_BEAN_MAP.put(SERVICE_PREFIX, ServiceProperties.class);
        PROPERTY_BEAN_MAP.put(UNDO_PREFIX, UndoProperties.class);
        PROPERTY_BEAN_MAP.put(COMPRESS_PREFIX, UndoCompressProperties.class);
        PROPERTY_BEAN_MAP.put(LOAD_BALANCE_PREFIX, LoadBalanceProperties.class);
        PROPERTY_BEAN_MAP.put(TCC_FENCE_PREFIX, CommonFenceConfig.class);
        PROPERTY_BEAN_MAP.put(SAGA_STATE_MACHINE_PREFIX, StateMachineConfig.class);
        PROPERTY_BEAN_MAP.put(SAGA_ASYNC_THREAD_POOL_PREFIX, SagaAsyncThreadPoolProperties.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}