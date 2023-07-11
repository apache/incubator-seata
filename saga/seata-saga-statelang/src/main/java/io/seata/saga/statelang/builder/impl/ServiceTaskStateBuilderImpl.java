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

package io.seata.saga.statelang.builder.impl;

import io.seata.saga.statelang.builder.ServiceTaskStateBuilder;
import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;

/**
 * Default implementation for {@link ServiceTaskStateBuilder}
 *
 * @author ptyin
 */
public class ServiceTaskStateBuilderImpl
        extends BaseStateBuilder<ServiceTaskStateBuilder, ServiceTaskState>
        implements ServiceTaskStateBuilder {

    protected ServiceTaskStateImpl state = new ServiceTaskStateImpl();

    @Override
    public ServiceTaskStateBuilder withServiceName(String serviceName) {
        state.setServiceName(serviceName);
        return this;
    }

    @Override
    public ServiceTaskStateBuilder withServiceMethod(String serviceMethod) {
        state.setServiceMethod(serviceMethod);
        return this;
    }

    @Override
    protected ServiceTaskStateBuilder getBuilder() {
        return this;
    }

    @Override
    protected ServiceTaskState getState() {
        return state;
    }
}
