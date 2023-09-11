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

import io.seata.saga.statelang.builder.prop.ServiceTaskPropertyBuilder;
import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.builder.prop.TaskPropertyBuilder;
import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract service task state builder to inherit.
 *
 * @author ptyin
 */
public abstract class AbstractServiceTaskStateBuilder
        <P extends BasicPropertyBuilder<P> & TaskPropertyBuilder<P> & ServiceTaskPropertyBuilder<P>,
                S extends ServiceTaskState>
        extends AbstractTaskStateBuilder<P, S>
        implements ServiceTaskPropertyBuilder<P> {

    @Override
    public P withServiceName(String serviceName) {
        ((ServiceTaskStateImpl) getState()).setServiceName(serviceName);
        return getPropertyBuilder();
    }

    @Override
    public P withServiceMethod(String serviceMethod) {
        ((ServiceTaskStateImpl) getState()).setServiceMethod(serviceMethod);
        return getPropertyBuilder();
    }

    @Override
    public P withServiceType(String serviceType) {
        ((ServiceTaskStateImpl) getState()).setServiceType(serviceType);
        return getPropertyBuilder();
    }

    @Override
    public P withParameterTypes(Collection<String> parameterTypes) {
        ((ServiceTaskStateImpl) getState()).setParameterTypes(new ArrayList<>(parameterTypes));
        return getPropertyBuilder();
    }

    @Override
    public P withAsync(boolean async) {
        ((ServiceTaskStateImpl) getState()).setAsync(async);
        return getPropertyBuilder();
    }
}
