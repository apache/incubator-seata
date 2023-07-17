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

import io.seata.saga.statelang.builder.ChoiceStateBuilder;
import io.seata.saga.statelang.builder.ServiceTaskStateBuilder;
import io.seata.saga.statelang.builder.StateMachineBuilder;
import io.seata.saga.statelang.builder.impl.StateMachineBuilderImpl;
import io.seata.saga.statelang.domain.StateMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Java stream-based builder tests.
 *
 * @author lorne.cl
 */
public class StateBuilderTests {
    @Test
    void testBuildStateMachine() {
        StateMachineBuilder stateMachineBuilder = new StateMachineBuilderImpl();
        StateMachine stateMachine = stateMachineBuilder
                .withName("simpleTestStateMachine")
                .withComment("测试状态机定义")
                .withVersion("0.0.2")
                .withStartState("FirstState")
                .withStates()
                    .build(ServiceTaskStateBuilder.class)
                        .withName("FirstState")
                        .withServiceName("demoService")
                        .withServiceMethod("foo")
                        .withNext("ChoiceState")
                        .and()
                    .build(ChoiceStateBuilder.class)
                        .withName("ChoiceState")
                        .put("[a] == 1", "SecondState")
                        .put("[a] == 2", "ThirdState")
                        .and()
                    .configure()
                .build();
        Assertions.assertNotNull(stateMachine);
    }
}
