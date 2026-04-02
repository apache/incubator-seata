/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.protocol;

/**
 * The type Unregister rm request.
 */
public class UnregisterRMRequest extends AbstractIdentifyRequest {

    public UnregisterRMRequest() {
        this(null, null);
    }

    public UnregisterRMRequest(String applicationId, String transactionServiceGroup) {
        super(applicationId, transactionServiceGroup);
    }

    private String resourceIds;

    public String getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String resourceIds) {
        this.resourceIds = resourceIds;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_UNREG_RM;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnregisterRMRequest{");
        sb.append("resourceIds='").append(resourceIds).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", applicationId='").append(applicationId).append('\'');
        sb.append(", transactionServiceGroup='").append(transactionServiceGroup).append('\'');
        sb.append(", extraData='").append(extraData).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
