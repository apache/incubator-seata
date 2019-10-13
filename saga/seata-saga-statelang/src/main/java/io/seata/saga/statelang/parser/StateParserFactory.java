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
package io.seata.saga.statelang.parser;

import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.parser.impl.ChoiceStateParser;
import io.seata.saga.statelang.parser.impl.CompensateSubStateMachineStateParser;
import io.seata.saga.statelang.parser.impl.CompensationTriggerStateParser;
import io.seata.saga.statelang.parser.impl.FailEndStateParser;
import io.seata.saga.statelang.parser.impl.ServiceTaskStateParser;
import io.seata.saga.statelang.parser.impl.SubStateMachineParser;
import io.seata.saga.statelang.parser.impl.SucceedEndStateParser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple factory of state parser
 * @author lorne.cl
 */
public class StateParserFactory {

    protected static Map<String, StateParser> stateParserMap = new ConcurrentHashMap<>();

    static{
        stateParserMap.put(DomainConstants.STATE_TYPE_SERVICE_TASK, new ServiceTaskStateParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_CHOICE, new ChoiceStateParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER, new CompensationTriggerStateParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_FAIL, new FailEndStateParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_SUCCEED, new SucceedEndStateParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_SUB_STATE_MACHINE, new SubStateMachineParser());
        stateParserMap.put(DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION, new CompensateSubStateMachineStateParser());
    }

    public static StateParser getStateParser(String stateType){

        return stateParserMap.get(stateType);
    }
}