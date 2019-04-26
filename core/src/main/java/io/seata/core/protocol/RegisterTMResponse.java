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
package io.seata.core.protocol;

import java.io.Serializable;

/**
 * The type Register tm response.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/15
 */
public class RegisterTMResponse extends AbstractIdentifyResponse implements Serializable {
    private static final long serialVersionUID = 3629846050062228749L;

    /**
     * Instantiates a new Register tm response.
     */
    public RegisterTMResponse() {
        this(true);
    }

    /**
     * Instantiates a new Register tm response.
     *
     * @param result the result
     */
    public RegisterTMResponse(boolean result) {
        super();
        setIdentified(result);
    }

    @Override
    public short getTypeCode() {
        return TYPE_REG_CLT_RESULT;
    }
}
