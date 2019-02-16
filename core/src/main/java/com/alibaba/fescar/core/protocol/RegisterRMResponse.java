/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol;

import java.io.Serializable;

/**
 * The type Register rm response.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/10 15:02
 * @FileName: RegisterRMResponse
 * @Description:
 */
public class RegisterRMResponse extends AbstractIdentifyResponse implements Serializable {
    private static final long serialVersionUID = 6391375605848221420L;

    /**
     * Instantiates a new Register rm response.
     */
    public RegisterRMResponse() {
        this(true);
    }

    /**
     * Instantiates a new Register rm response.
     *
     * @param result the result
     */
    public RegisterRMResponse(boolean result) {
        super();
        setIdentified(result);
    }

    @Override
    public short getTypeCode() {
        return TYPE_REG_RM_RESULT;
    }
}
