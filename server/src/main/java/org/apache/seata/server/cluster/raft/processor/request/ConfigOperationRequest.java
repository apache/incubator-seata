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
package org.apache.seata.server.cluster.raft.processor.request;

import org.apache.seata.server.cluster.raft.execute.config.ConfigOperationType;

import java.io.Serializable;
import java.util.Map;


public class ConfigOperationRequest implements Serializable {
    private static final long serialVersionUID = -1149573667621259458L;
    private ConfigOperationType optType;
    private String namespace;
    private String dataId;
    private String key;
    private Object value;

    public ConfigOperationRequest() {
    }

    public ConfigOperationRequest(ConfigOperationType optType, String namespace, String dataId) {
        this.optType = optType;
        this.namespace = namespace;
        this.dataId = dataId;
    }

    public ConfigOperationRequest(ConfigOperationType optType,String namespace, String dataId, String key) {
        this.optType = optType;
        this.namespace = namespace;
        this.dataId = dataId;
        this.key = key;
    }

    public ConfigOperationRequest(ConfigOperationType optType, String namespace, String dataId, String key, Object value) {
        this.optType = optType;
        this.namespace = namespace;
        this.dataId = dataId;
        this.key = key;
        this.value = value;
    }

    public static ConfigOperationRequest buildGetRequest(String namespace, String dataId, String key) {
        return new ConfigOperationRequest(ConfigOperationType.GET, namespace, dataId, key);
    }

    public static ConfigOperationRequest buildPutRequest(String namespace, String dataId, String key, String value) {
        return new ConfigOperationRequest(ConfigOperationType.PUT, namespace, dataId, key, value);
    }

    public static ConfigOperationRequest buildDeleteRequest(String namespace, String dataId, String key) {
        return new ConfigOperationRequest(ConfigOperationType.DELETE, namespace, dataId, key);
    }
    public static ConfigOperationRequest buildDeleteAllRequest(String namespace, String dataId) {
        return new ConfigOperationRequest(ConfigOperationType.DELETE_ALL, namespace, dataId);
    }

    public static ConfigOperationRequest buildGetAllRequest(String namespace, String dataId) {
        return new ConfigOperationRequest(ConfigOperationType.GET_ALL, namespace, dataId);
    }

    public static ConfigOperationRequest buildUploadRequest(String namespace, String dataId, Map<String, Object> configMap) {
        return new ConfigOperationRequest(ConfigOperationType.UPLOAD, namespace, dataId, null, configMap);
    }

    public static ConfigOperationRequest buildGetNamespaces() {
        return new ConfigOperationRequest(ConfigOperationType.GET_NAMESPACES, null, null);
    }

    public static ConfigOperationRequest buildGetDataIds(String namespace) {
        return new ConfigOperationRequest(ConfigOperationType.GET_DATA_IDS, namespace, null);
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

    @Override
    public String toString() {
        return "ConfigOperationRequest{" +
                "optType=" + optType +
                ", namespace='" + namespace + '\'' +
                ", dataId='" + dataId + '\'' +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
