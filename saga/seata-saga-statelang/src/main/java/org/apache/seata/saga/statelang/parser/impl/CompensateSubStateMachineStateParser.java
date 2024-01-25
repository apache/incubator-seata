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
package org.apache.seata.saga.statelang.parser.impl;

import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ServiceTaskState;
import org.apache.seata.saga.statelang.domain.impl.CompensateSubStateMachineStateImpl;
import org.apache.seata.saga.statelang.parser.StateParser;
import org.springframework.util.StringUtils;

/**
 * CompensateSubStateMachineState Parser
 *
 */
public class CompensateSubStateMachineStateParser extends AbstractTaskStateParser
    implements StateParser<ServiceTaskState> {

    @Override
    public ServiceTaskState parse(Object node) {

        CompensateSubStateMachineStateImpl compensateSubStateMachineState = new CompensateSubStateMachineStateImpl();
        compensateSubStateMachineState.setForCompensation(true);
        if (node != null) {
            parseTaskAttributes(compensateSubStateMachineState, node);
        }
        if (StringUtils.isEmpty(compensateSubStateMachineState.getName())) {
            compensateSubStateMachineState.setName(
                DomainConstants.COMPENSATE_SUB_MACHINE_STATE_NAME_PREFIX + compensateSubStateMachineState.hashCode());
        }
        return compensateSubStateMachineState;
    }
}
