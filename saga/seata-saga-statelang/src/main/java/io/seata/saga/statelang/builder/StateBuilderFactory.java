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

package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.builder.impl.ChoiceStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.CompensateSubStateMachineStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.CompensationTriggerStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.FailEndStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.ForkStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.JoinStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.ScriptTaskStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.ServiceTaskStateBuilderImpl;
import io.seata.saga.statelang.builder.impl.SubStateMachineBuilderImpl;
import io.seata.saga.statelang.builder.impl.SuccessEndStateBuilderImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * State builder factory.
 *
 * @author ptyin
 */
public class StateBuilderFactory {
    protected static Map<Class<? extends StateBuilder<?>>, Class<? extends StateBuilder<?>>> stateBuilderMap
            = new HashMap<>();

    static {
        stateBuilderMap.put(ChoiceStateBuilder.class, ChoiceStateBuilderImpl.class);
        stateBuilderMap.put(CompensateSubStateMachineStateBuilder.class,
                CompensateSubStateMachineStateBuilderImpl.class);
        stateBuilderMap.put(CompensationTriggerStateBuilder.class,
                CompensationTriggerStateBuilderImpl.class);
        stateBuilderMap.put(FailEndStateBuilder.class, FailEndStateBuilderImpl.class);
        stateBuilderMap.put(ForkStateBuilder.class, ForkStateBuilderImpl.class);
        stateBuilderMap.put(JoinStateBuilder.class, JoinStateBuilderImpl.class);
        stateBuilderMap.put(ScriptTaskStateBuilder.class, ScriptTaskStateBuilderImpl.class);
        stateBuilderMap.put(ServiceTaskStateBuilder.class, ServiceTaskStateBuilderImpl.class);
        stateBuilderMap.put(SubStateMachineBuilder.class, SubStateMachineBuilderImpl.class);
        stateBuilderMap.put(SuccessEndStateBuilder.class, SuccessEndStateBuilderImpl.class);
    }

    public static StateBuilder<?> getStateBuilder(Class<? extends StateBuilder<?>> clazz)
            throws InstantiationException, IllegalAccessException {
        Class<? extends StateBuilder<?>> implClazz = stateBuilderMap.getOrDefault(clazz, clazz);
        return implClazz.newInstance();
    }
}
