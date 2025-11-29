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

import org.apache.seata.saga.statelang.domain.ChoiceState;
import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for ChoiceStateImpl
 */
public class ChoiceStateImplTest {

    @Test
    public void testConstructor() {
        ChoiceStateImpl choiceState = new ChoiceStateImpl();
        assertEquals(StateType.CHOICE, choiceState.getType());
    }

    @Test
    public void testChoices() {
        ChoiceStateImpl choiceState = new ChoiceStateImpl();
        List<ChoiceState.Choice> choices = new ArrayList<>();
        ChoiceState.Choice choice = new ChoiceStateImpl.ChoiceImpl();
        choices.add(choice);

        choiceState.setChoices(choices);
        assertNotNull(choiceState.getChoices());
        assertEquals(1, choiceState.getChoices().size());
        Assertions.assertSame(choice, choiceState.getChoices().get(0));
    }

    @Test
    public void testDefaultChoice() {
        ChoiceStateImpl choiceState = new ChoiceStateImpl();
        String defaultChoice = "defaultNextState";

        choiceState.setDefaultChoice(defaultChoice);
        assertEquals(defaultChoice, choiceState.getDefault());
    }

    @Test
    public void testChoiceEvaluators() {
        ChoiceStateImpl choiceState = new ChoiceStateImpl();
        Map<Object, String> evaluators = new HashMap<>();
        Object key = new Object();
        String value = "nextState1";
        evaluators.put(key, value);

        choiceState.setChoiceEvaluators(evaluators);
        assertNotNull(choiceState.getChoiceEvaluators());
        assertEquals(1, choiceState.getChoiceEvaluators().size());
        assertEquals(value, choiceState.getChoiceEvaluators().get(key));
    }

    @Test
    public void testChoiceImpl() {
        ChoiceStateImpl.ChoiceImpl choice = new ChoiceStateImpl.ChoiceImpl();
        String expression = "${a > b}";
        String next = "nextState2";

        choice.setExpression(expression);
        choice.setNext(next);

        assertEquals(expression, choice.getExpression());
        assertEquals(next, choice.getNext());
    }

    @Test
    public void testNullValues() {
        ChoiceStateImpl choiceState = new ChoiceStateImpl();

        choiceState.setChoices(null);
        Assertions.assertNull(choiceState.getChoices());

        choiceState.setDefaultChoice(null);
        Assertions.assertNull(choiceState.getDefault());

        choiceState.setChoiceEvaluators(null);
        Assertions.assertNull(choiceState.getChoiceEvaluators());

        ChoiceStateImpl.ChoiceImpl choice = new ChoiceStateImpl.ChoiceImpl();
        choice.setExpression(null);
        choice.setNext(null);
        Assertions.assertNull(choice.getExpression());
        Assertions.assertNull(choice.getNext());
    }
}
