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
 * The type Register rm request.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/10
 */
public class RegisterRMRequest extends AbstractIdentifyRequest implements Serializable {

    /**
     * Instantiates a new Register rm request.
     */
    public RegisterRMRequest() {
        this(null, null);
    }

    /**
     * Instantiates a new Register rm request.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public RegisterRMRequest(String applicationId, String transactionServiceGroup) {
        super(applicationId, transactionServiceGroup);
    }

    private String resourceIds;

    /**
     * Gets resource ids.
     *
     * @return the resource ids
     */
    public String getResourceIds() {
        return resourceIds;
    }

    /**
     * Sets resource ids.
     *
     * @param resourceIds the resource ids
     */
    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_REG_RM;
    }

    @Override
    public String toString() {
        return "RegisterRMRequest{" +
                "resourceIds='" + resourceIds + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", transactionServiceGroup='" + transactionServiceGroup + '\'' +
                '}';
    }
}
