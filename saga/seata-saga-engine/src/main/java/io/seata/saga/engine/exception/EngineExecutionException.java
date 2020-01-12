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
package io.seata.saga.engine.exception;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;

/**
 * StateMachineEngine Execution Exception
 *
 * @author lorne.cl
 */
public class EngineExecutionException extends FrameworkException {

    private String stateName;
    private String stateMachineName;
    private String stateMachineInstanceId;
    private String stateInstanceId;

    public EngineExecutionException() {
    }

    public EngineExecutionException(FrameworkErrorCode err) {
        super(err);
    }

    public EngineExecutionException(String msg) {
        super(msg);
    }

    public EngineExecutionException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    public EngineExecutionException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    public EngineExecutionException(Throwable th) {
        super(th);
    }

    public EngineExecutionException(Throwable th, String msg) {
        super(th, msg);
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getStateMachineName() {
        return stateMachineName;
    }

    public void setStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
    }

    public String getStateMachineInstanceId() {
        return stateMachineInstanceId;
    }

    public void setStateMachineInstanceId(String stateMachineInstanceId) {
        this.stateMachineInstanceId = stateMachineInstanceId;
    }

    public String getStateInstanceId() {
        return stateInstanceId;
    }

    public void setStateInstanceId(String stateInstanceId) {
        this.stateInstanceId = stateInstanceId;
    }
}