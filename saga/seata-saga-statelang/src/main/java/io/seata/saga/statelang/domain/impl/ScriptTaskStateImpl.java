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

import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ScriptTaskState;

/**
 * A state used to execute script such as groovy
 *
 * @author lorne.cl
 */
public class ScriptTaskStateImpl extends AbstractTaskState implements ScriptTaskState {

    private static final String DEFAULT_SCRIPT_TYPE = "groovy";

    private String scriptType = DEFAULT_SCRIPT_TYPE;

    private String scriptContent;

    public ScriptTaskStateImpl() {
        setType(DomainConstants.STATE_TYPE_SCRIPT_TASK);
    }

    @Override
    public String getScriptType() {
        return this.scriptType;
    }

    @Override
    public String getScriptContent() {
        return this.scriptContent;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }
}