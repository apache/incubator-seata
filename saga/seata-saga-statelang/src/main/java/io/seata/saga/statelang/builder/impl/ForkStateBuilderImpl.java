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

import io.seata.common.util.CollectionUtils;
import io.seata.saga.statelang.builder.BuildException;
import io.seata.saga.statelang.builder.ForkStateBuilder;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.impl.ForkStateImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Default implementation for {@link ForkStateBuilder}
 *
 * @author ptyin
 */
public class ForkStateBuilderImpl
        extends BaseStateBuilder<ForkStateBuilder, ForkState>
        implements ForkStateBuilder {

    protected ForkStateImpl state = new ForkStateImpl();

    @Override
    public ForkStateBuilder withBranches(Collection<String> branches) {
        if (CollectionUtils.isEmpty(branches)) {
            throw new BuildException("Branches of fork state should not be empty");
        }
        if (new HashSet<>(branches).size() < branches.size()) {
            throw new BuildException("Branches of fork state should not be same.");
        }
        state.setBranches(new ArrayList<>(branches));
        return this;
    }

    @Override
    public ForkStateBuilder withParallel(int parallel) {
        state.setParallel(parallel);
        return this;
    }

    @Override
    public ForkStateBuilder withAwaitTimeout(int awaitTimeout) {
        state.setAwaitTimeout(awaitTimeout);
        return this;
    }

    @Override
    protected ForkStateBuilder getPropertyBuilder() {
        return this;
    }

    @Override
    protected ForkState getState() {
        return state;
    }
}
