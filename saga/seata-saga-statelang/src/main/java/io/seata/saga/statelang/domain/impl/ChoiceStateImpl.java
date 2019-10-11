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

import io.seata.saga.statelang.domain.ChoiceState;
import io.seata.saga.statelang.domain.DomainConstants;
import java.util.List;
import java.util.Map;

/**
 * Single selection status
 * @author lorne.cl
 */
public class ChoiceStateImpl extends BaseState implements ChoiceState {

    private List<Choice>         choices;
    private String               defaultChoice;
    /** key: Evaluator, value: Next **/
    private Map<Object, String>  choiceEvaluators;

    public ChoiceStateImpl() {
        setType(DomainConstants.STATE_TYPE_CHOICE);
    }

    @Override
    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    @Override
    public String getDefault() {
        return defaultChoice;
    }

    public void setDefaultChoice(String defaultChoice) {
        this.defaultChoice = defaultChoice;
    }

    public Map<Object, String> getChoiceEvaluators() {
        return choiceEvaluators;
    }

    public void setChoiceEvaluators(Map<Object, String> choiceEvaluators) {
        this.choiceEvaluators = choiceEvaluators;
    }

    public static class ChoiceImpl implements ChoiceState.Choice {

        private String expression;
        private String next;

        @Override
        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        @Override
        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }
    }
}