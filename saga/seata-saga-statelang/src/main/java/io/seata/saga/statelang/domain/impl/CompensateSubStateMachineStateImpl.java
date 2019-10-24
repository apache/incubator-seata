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
package io.seata.saga.statelang.domain.impl;

import io.seata.saga.statelang.domain.CompensateSubStateMachineState;
import io.seata.saga.statelang.domain.DomainConstants;

/**
 * Used to compensate the state of the sub state machine, inherited from ServiceTaskState
 * @author lorne.cl
 */
public class CompensateSubStateMachineStateImpl extends ServiceTaskStateImpl implements CompensateSubStateMachineState {
    public CompensateSubStateMachineStateImpl() {
        setType(DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION);
    }
}