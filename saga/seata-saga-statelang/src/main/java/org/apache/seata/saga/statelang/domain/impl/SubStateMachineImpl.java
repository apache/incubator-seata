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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.domain.SubStateMachine;
import org.apache.seata.saga.statelang.domain.TaskState;

/**
 * sub state machine
 *
 */
public class SubStateMachineImpl extends ServiceTaskStateImpl implements SubStateMachine {

    private String stateMachineName;

    private TaskState compensateStateObject;

    public SubStateMachineImpl() {
        setType(StateType.SUB_STATE_MACHINE);
    }

    @Override
    public String getStateMachineName() {
        return stateMachineName;
    }

    public void setStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
    }

    @Override
    public TaskState getCompensateStateObject() {
        return compensateStateObject;
    }

    public void setCompensateStateObject(TaskState compensateStateObject) {
        this.compensateStateObject = compensateStateObject;
    }
}
