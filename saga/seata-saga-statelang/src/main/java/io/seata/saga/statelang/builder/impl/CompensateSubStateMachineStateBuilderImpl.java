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

import io.seata.saga.statelang.builder.CompensateSubStateMachineStateBuilder;
import io.seata.saga.statelang.domain.CompensateSubStateMachineState;
import io.seata.saga.statelang.domain.impl.CompensateSubStateMachineStateImpl;

/**
 * Default implementation for {@link CompensateSubStateMachineStateBuilder}
 *
 * @author ptyin
 */
public class CompensateSubStateMachineStateBuilderImpl
        extends AbstractServiceTaskStateBuilder<CompensateSubStateMachineStateBuilder, CompensateSubStateMachineState>
        implements CompensateSubStateMachineStateBuilder {

    protected CompensateSubStateMachineStateImpl state;

    @Override
    protected CompensateSubStateMachineStateBuilder getPropertyBuilder() {
        return this;
    }

    @Override
    protected CompensateSubStateMachineState getState() {
        if (state == null) {
            state = new CompensateSubStateMachineStateImpl();
        }
        return state;
    }
}
