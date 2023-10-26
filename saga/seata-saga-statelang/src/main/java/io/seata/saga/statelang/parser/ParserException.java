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

package io.seata.saga.statelang.parser;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;


/**
 * StateMachine parser exception
 *
 * @author ptyin
 */
public class ParserException extends FrameworkException {

    private String stateName;
    private String stateMachineName;

    public ParserException() {
    }

    public ParserException(FrameworkErrorCode err) {
        super(err);
    }

    public ParserException(String msg) {
        super(msg);
    }

    public ParserException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    public ParserException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    public ParserException(Throwable th) {
        super(th);
    }

    public ParserException(Throwable th, String msg) {
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
}
