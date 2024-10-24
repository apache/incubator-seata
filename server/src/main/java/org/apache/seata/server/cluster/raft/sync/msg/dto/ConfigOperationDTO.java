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
package org.apache.seata.server.cluster.raft.sync.msg.dto;

import java.io.Serializable;

import org.apache.seata.server.cluster.raft.execute.config.ConfigOperationType;
import org.apache.seata.server.cluster.raft.processor.request.ConfigOperationRequest;

public class ConfigOperationDTO implements Serializable {
    private static final long serialVersionUID = -1237293571963636954L;

    private ConfigOperationType optType;
    private String namespace;
    private String dataId;
    private String key;
    private Object value;

    public ConfigOperationDTO() {
    }

    public ConfigOperationDTO(ConfigOperationType optType, String namespace, String dataId, String key, Object value) {
        this.optType = optType;
        this.namespace = namespace;
        this.dataId = dataId;
        this.key = key;
        this.value = value;
    }

    public ConfigOperationType getOptType() {
        return optType;
    }

    public void setOptType(ConfigOperationType optType) {
        this.optType = optType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static ConfigOperationDTO convertConfigRequest2Dto(ConfigOperationRequest request) {
        return new ConfigOperationDTO(request.getOptType(), request.getNamespace(), request.getDataId(), request.getKey(), request.getValue());
    }

    @Override
    public String toString() {
        return "ConfigOperationDTO{" +
                "optType=" + optType +
                ", namespace='" + namespace + '\'' +
                ", dataId='" + dataId + '\'' +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
