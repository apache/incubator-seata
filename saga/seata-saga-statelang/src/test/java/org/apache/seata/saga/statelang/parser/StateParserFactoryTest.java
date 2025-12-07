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

import org.apache.seata.saga.statelang.domain.StateType;
import org.apache.seata.saga.statelang.parser.impl.ChoiceStateParser;
import org.apache.seata.saga.statelang.parser.impl.CompensateSubStateMachineStateParser;
import org.apache.seata.saga.statelang.parser.impl.CompensationTriggerStateParser;
import org.apache.seata.saga.statelang.parser.impl.FailEndStateParser;
import org.apache.seata.saga.statelang.parser.impl.ScriptTaskStateParser;
import org.apache.seata.saga.statelang.parser.impl.ServiceTaskStateParser;
import org.apache.seata.saga.statelang.parser.impl.SubStateMachineParser;
import org.apache.seata.saga.statelang.parser.impl.SucceedEndStateParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * test StateParserFactory
 */
public class StateParserFactoryTest {

    @Test
    public void testGetStateParser_ExistingTypes() {
        StateParser serviceTaskParser = StateParserFactory.getStateParser(StateType.SERVICE_TASK);
        assertNotNull(serviceTaskParser, "ServiceTaskStateParser should not be null");
        assertTrue(serviceTaskParser instanceof ServiceTaskStateParser, "Parser should be ServiceTaskStateParser");

        StateParser choiceParser = StateParserFactory.getStateParser(StateType.CHOICE);
        assertNotNull(choiceParser, "ChoiceStateParser should not be null");
        assertTrue(choiceParser instanceof ChoiceStateParser, "Parser should be ChoiceStateParser");

        StateParser compensationTriggerParser = StateParserFactory.getStateParser(StateType.COMPENSATION_TRIGGER);
        assertNotNull(compensationTriggerParser, "CompensationTriggerStateParser should not be null");
        assertTrue(
                compensationTriggerParser instanceof CompensationTriggerStateParser,
                "Parser should be CompensationTriggerStateParser");

        StateParser failEndParser = StateParserFactory.getStateParser(StateType.FAIL);
        assertNotNull(failEndParser, "FailEndStateParser should not be null");
        assertTrue(failEndParser instanceof FailEndStateParser, "Parser should be FailEndStateParser");

        StateParser succeedEndParser = StateParserFactory.getStateParser(StateType.SUCCEED);
        assertNotNull(succeedEndParser, "SucceedEndStateParser should not be null");
        assertTrue(succeedEndParser instanceof SucceedEndStateParser, "Parser should be SucceedEndStateParser");

        StateParser subStateMachineParser = StateParserFactory.getStateParser(StateType.SUB_STATE_MACHINE);
        assertNotNull(subStateMachineParser, "SubStateMachineParser should not be null");
        assertTrue(subStateMachineParser instanceof SubStateMachineParser, "Parser should be SubStateMachineParser");

        StateParser subCompensationParser = StateParserFactory.getStateParser(StateType.SUB_MACHINE_COMPENSATION);
        assertNotNull(subCompensationParser, "CompensateSubStateMachineStateParser should not be null");
        assertTrue(
                subCompensationParser instanceof CompensateSubStateMachineStateParser,
                "Parser should be CompensateSubStateMachineStateParser");

        StateParser scriptTaskParser = StateParserFactory.getStateParser(StateType.SCRIPT_TASK);
        assertNotNull(scriptTaskParser, "ScriptTaskStateParser should not be null");
        assertTrue(scriptTaskParser instanceof ScriptTaskStateParser, "Parser should be ScriptTaskStateParser");
    }
}
