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
 * The type Register tm request.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/15
 */
public class RegisterTMRequest extends AbstractIdentifyRequest implements Serializable {
    private static final long serialVersionUID = -5929081344190543690L;

    /**
     * Instantiates a new Register tm request.
     */
    public RegisterTMRequest() {
        this(null, null);
    }

    /**
     * Instantiates a new Register tm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param extraData               the extra data
     */
    public RegisterTMRequest(String applicationId, String transactionServiceGroup, String extraData) {
        super(applicationId, transactionServiceGroup, extraData);

    }

    /**
     * Instantiates a new Register tm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public RegisterTMRequest(String applicationId, String transactionServiceGroup) {
        super(applicationId, transactionServiceGroup);
    }

    @Override
    public short getTypeCode() {
        return TYPE_REG_CLT;
    }

    @Override
    public String toString() {
        return "RegisterTMRequest{" +
            "applicationId='" + applicationId + '\'' +
            ", transactionServiceGroup='" + transactionServiceGroup + '\'' +
            '}';
    }
}
