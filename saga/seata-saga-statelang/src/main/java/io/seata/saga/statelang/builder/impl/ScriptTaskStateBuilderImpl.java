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

import io.seata.saga.statelang.builder.ScriptTaskStateBuilder;
import io.seata.saga.statelang.domain.ScriptTaskState;
import io.seata.saga.statelang.domain.impl.ScriptTaskStateImpl;

/**
 * Default implementation for {@link ScriptTaskStateBuilder}
 *
 * @author ptyin
 */
public class ScriptTaskStateBuilderImpl
        extends AbstractTaskStateBuilder<ScriptTaskStateBuilder, ScriptTaskState>
        implements ScriptTaskStateBuilder {

    protected ScriptTaskStateImpl state;

    public ScriptTaskStateBuilderImpl() {
        state.setForCompensation(false);
        state.setForUpdate(false);
        state.setPersist(false);
    }

    @Override
    public ScriptTaskStateBuilder withScriptType(String scriptType) {
        state.setScriptType(scriptType);
        return this;
    }

    @Override
    public ScriptTaskStateBuilder withScriptContent(String scriptContent) {
        state.setScriptContent(scriptContent);
        return this;
    }

    @Override
    protected ScriptTaskStateBuilder getPropertyBuilder() {
        return this;
    }

    @Override
    protected ScriptTaskState getState() {
        if (state == null) {
            state = new ScriptTaskStateImpl();
        }
        return state;
    }
}
