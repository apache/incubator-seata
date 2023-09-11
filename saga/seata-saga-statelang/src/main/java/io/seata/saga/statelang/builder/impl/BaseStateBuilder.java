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

import io.seata.saga.statelang.builder.StateBuilder;
import io.seata.saga.statelang.builder.StatesConfigurer;
import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.BaseState;

/**
 * Base state builder to inherit.
 *
 * @param <B> builder type
 * @param <S> state type
 * @author ptyin
 */
public abstract class BaseStateBuilder<B extends BasicPropertyBuilder<B>, S extends State>
        implements StateBuilder<S>, BasicPropertyBuilder<B> {
    private StatesConfigurer parent;

    @Override
    public B withName(String name) {
        ((BaseState) getState()).setName(name);
        return getPropertyBuilder();
    }

    @Override
    public B withComment(String comment) {
        ((BaseState) getState()).setComment(comment);
        return getPropertyBuilder();
    }

    @Override
    public B withNext(String next) {
        ((BaseState) getState()).setNext(next);
        return getPropertyBuilder();
    }

    @Override
    public S build() {
        return getState();
    }

    @Override
    public StatesConfigurer and() {
        parent.add(getState());
        return parent;
    }

    public void setParent(StatesConfigurer parent) {
        this.parent = parent;
    }

    protected abstract B getPropertyBuilder();

    protected abstract S getState();
}
