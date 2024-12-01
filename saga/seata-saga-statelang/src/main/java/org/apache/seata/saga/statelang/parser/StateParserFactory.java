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
package org.apache.seata.saga.statelang.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.parser.impl.ChoiceStateParser;
import org.apache.seata.saga.statelang.parser.impl.CompensateSubStateMachineStateParser;
import org.apache.seata.saga.statelang.parser.impl.CompensationTriggerStateParser;
import org.apache.seata.saga.statelang.parser.impl.FailEndStateParser;
import org.apache.seata.saga.statelang.parser.impl.ScriptTaskStateParser;
import org.apache.seata.saga.statelang.parser.impl.ServiceTaskStateParser;
import org.apache.seata.saga.statelang.parser.impl.SubStateMachineParser;
import org.apache.seata.saga.statelang.parser.impl.SucceedEndStateParser;

/**
 * A simple factory of state parser
 *
 */
public class StateParserFactory {

    protected static Map<StateType, StateParser> stateParserMap = new ConcurrentHashMap<>();

    static {
        stateParserMap.put(StateType.SERVICE_TASK, new ServiceTaskStateParser());
        stateParserMap.put(StateType.CHOICE, new ChoiceStateParser());
        stateParserMap.put(StateType.COMPENSATION_TRIGGER, new CompensationTriggerStateParser());
        stateParserMap.put(StateType.FAIL, new FailEndStateParser());
        stateParserMap.put(StateType.SUCCEED, new SucceedEndStateParser());
        stateParserMap.put(StateType.SUB_STATE_MACHINE, new SubStateMachineParser());
        stateParserMap.put(StateType.SUB_MACHINE_COMPENSATION,
            new CompensateSubStateMachineStateParser());
        stateParserMap.put(StateType.SCRIPT_TASK, new ScriptTaskStateParser());
    }

    public static StateParser getStateParser(StateType stateType) {
        return stateParserMap.get(stateType);
    }
}
