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

import io.seata.saga.statelang.builder.FailEndStateBuilder;
import io.seata.saga.statelang.domain.FailEndState;
import io.seata.saga.statelang.domain.impl.FailEndStateImpl;

/**
 * Default implementation for {@link FailEndStateBuilder}
 *
 * @author ptyin
 */
public class FailEndStateBuilderImpl
        extends BaseStateBuilder<FailEndStateBuilder, FailEndState>
        implements FailEndStateBuilder {

    protected FailEndStateImpl state = new FailEndStateImpl();

    @Override
    public FailEndStateBuilder withErrorCode(String errorCode) {
        state.setErrorCode(errorCode);
        return this;
    }

    @Override
    public FailEndStateBuilder withMessage(String message) {
        state.setMessage(message);
        return this;
    }

    @Override
    protected FailEndStateBuilder getPropertyBuilder() {
        return this;
    }

    @Override
    protected FailEndState getState() {
        return state;
    }
}
