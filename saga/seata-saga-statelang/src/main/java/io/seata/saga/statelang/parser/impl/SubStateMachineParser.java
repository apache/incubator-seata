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
package io.seata.saga.statelang.parser.impl;

import io.seata.saga.statelang.domain.ServiceTaskState;
import io.seata.saga.statelang.domain.SubStateMachine;
import io.seata.saga.statelang.domain.impl.SubStateMachineImpl;
import io.seata.saga.statelang.parser.StateParser;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * SubStateMachineParser
 * @author lorne.cl
 */
public class SubStateMachineParser extends AbstractTaskStateParser implements StateParser<SubStateMachine> {

    @Override
    public SubStateMachine parse(Object node) {

        SubStateMachineImpl subStateMachine = new SubStateMachineImpl();

        parseTaskAttributes(subStateMachine, node);

        Map<String, Object> nodeMap = (Map<String, Object>)node;
        subStateMachine.setStateMachineName((String)nodeMap.get("StateMachineName"));

        if(StringUtils.isEmpty(subStateMachine.getCompensateState())){
            //build default SubStateMachine compensate state
            CompensateSubStateMachineStateParser compensateSubStateMachineStateParser = new CompensateSubStateMachineStateParser();
            ServiceTaskState subStateMachineCompenState = compensateSubStateMachineStateParser.parse(null);
            subStateMachine.setCompensateStateObject(subStateMachineCompenState);
            subStateMachine.setCompensateState(subStateMachineCompenState.getName());
        }

        return subStateMachine;
    }
}