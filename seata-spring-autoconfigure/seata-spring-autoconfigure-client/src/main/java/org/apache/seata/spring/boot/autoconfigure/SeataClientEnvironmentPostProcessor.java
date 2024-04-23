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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.rm.fence.SpringFenceConfig;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.spring.boot.autoconfigure.properties.SagaAsyncThreadPoolProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.SeataTccProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.LoadBalanceProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.LockProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.ServiceProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.TmProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.UndoCompressProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.client.UndoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.COMPRESS_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.LOAD_BALANCE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.LOCK_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SAGA_ASYNC_THREAD_POOL_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SAGA_STATE_MACHINE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.TCC_FENCE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.TCC_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.UNDO_PREFIX;


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
        PROPERTY_BEAN_MAP.put(TCC_FENCE_PREFIX, SpringFenceConfig.class);
        PROPERTY_BEAN_MAP.put(SAGA_STATE_MACHINE_PREFIX, StateMachineConfig.class);
        PROPERTY_BEAN_MAP.put(SAGA_ASYNC_THREAD_POOL_PREFIX, SagaAsyncThreadPoolProperties.class);
        PROPERTY_BEAN_MAP.put(TCC_PREFIX, SeataTccProperties.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
