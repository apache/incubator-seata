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

package io.seata.saga.statelang.builder.impl;

import io.seata.saga.statelang.builder.ChoiceStateBuilder;
import io.seata.saga.statelang.domain.ChoiceState;
import io.seata.saga.statelang.domain.impl.ChoiceStateImpl;

import java.util.ArrayList;

/**
 * Default implementation for {@link ChoiceStateBuilder}
 *
 * @author ptyin
 */
public class ChoiceStateBuilderImpl
        extends BaseStateBuilder<ChoiceStateBuilder, ChoiceState>
        implements ChoiceStateBuilder {

    protected ChoiceStateImpl state = new ChoiceStateImpl();

    public ChoiceStateBuilderImpl() {
        state.setChoices(new ArrayList<>());
    }

    @Override
    public ChoiceStateBuilder withChoice(String expression, String next) {
        ChoiceStateImpl.ChoiceImpl choice = new ChoiceStateImpl.ChoiceImpl();
        choice.setExpression(expression);
        choice.setNext(next);
        state.getChoices().add(choice);
        return this;
    }

    @Override
    public ChoiceStateBuilder withDefault(String defaultChoice) {
        state.setDefaultChoice(defaultChoice);
        return this;
    }

    @Override
    protected ChoiceStateBuilder getBuilder() {
        return this;
    }

    @Override
    protected ChoiceState getState() {
        return state;
    }
}
