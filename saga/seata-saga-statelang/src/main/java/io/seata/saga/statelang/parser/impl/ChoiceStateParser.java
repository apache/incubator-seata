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

import io.seata.saga.statelang.domain.ChoiceState;
import io.seata.saga.statelang.domain.ChoiceState.Choice;
import io.seata.saga.statelang.domain.impl.ChoiceStateImpl;
import io.seata.saga.statelang.domain.impl.ChoiceStateImpl.ChoiceImpl;
import io.seata.saga.statelang.parser.StateParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Single item selection state parser
 * @author lorne.cl
 */
public class ChoiceStateParser extends BaseStatePaser implements StateParser<ChoiceState> {

    @Override
    public ChoiceState parse(Object node) {

        ChoiceStateImpl choiceState = new ChoiceStateImpl();
        parseBaseAttributes(choiceState, node);

        Map<String, Object> nodeMap = (Map<String, Object>)node;
        List<Object> choiceObjList = (List<Object>)nodeMap.get("Choices");
        List<Choice> choiceStateList = new ArrayList<>(choiceObjList.size());
        for(Object choiceObj : choiceObjList){

            Map<String, Object> choiceObjMap = (Map<String, Object>)choiceObj;
            ChoiceImpl choice = new ChoiceImpl();
            choice.setExpression((String)choiceObjMap.get("Expression"));
            choice.setNext((String)choiceObjMap.get("Next"));

            choiceStateList.add(choice);
        }
        choiceState.setChoices(choiceStateList);

        choiceState.setDefaultChoice((String)nodeMap.get("Default"));

        return choiceState;
    }
}