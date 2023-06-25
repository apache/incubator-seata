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
package io.seata.server.console.vo;

import io.seata.core.rpc.ClientInfo;
import io.seata.core.rpc.netty.NettyPoolKey;

import java.util.Set;

/**
 * @author TheR1sing3un
 * @date 2022/7/26 22:03
 * @description Client View Object
 */

public class ClientVO {

    private String resourceId;

    private String clientId;

    private NettyPoolKey.TransactionRole clientRole;

    private String version;

    private String applicationId;

    private String transactionServiceGroup;

    private Set<String> resourceSets;

    public ClientVO(String resourceId, String clientId, NettyPoolKey.TransactionRole clientRole, String version, String applicationId, String transactionServiceGroup, Set<String> resourceSets) {
        this.resourceId = resourceId;
        this.clientId = clientId;
        this.clientRole = clientRole;
        this.version = version;
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.resourceSets = resourceSets;
    }

    public ClientVO(ClientInfo clientInfo) {
        this.resourceId = clientInfo.getResourceId();
        this.clientId = clientInfo.getClientId();
        this.clientRole = clientInfo.getClientRole();
        this.version = clientInfo.getVersion();
        this.applicationId = clientInfo.getApplicationId();
        this.transactionServiceGroup = clientInfo.getTransactionServiceGroup();
        this.resourceSets = clientInfo.getResourceSets();
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public NettyPoolKey.TransactionRole getClientRole() {
        return clientRole;
    }

    public void setClientRole(NettyPoolKey.TransactionRole clientRole) {
        this.clientRole = clientRole;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    public Set<String> getResourceSets() {
        return resourceSets;
    }

    public void setResourceSets(Set<String> resourceSets) {
        this.resourceSets = resourceSets;
    }
}
