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


public class ConfigOperationRequest implements Serializable {
    private static final long serialVersionUID = -1149573667621259458L;
    private ConfigOperationType optType;
    private String group;
    private String key;
    private String value;

    public ConfigOperationRequest() {
    }

    public ConfigOperationRequest(ConfigOperationType optType, String group) {
        this.optType = optType;
        this.group = group;
    }

    public ConfigOperationRequest(ConfigOperationType optType, String group, String key) {
        this.optType = optType;
        this.group = group;
        this.key = key;
    }

    public ConfigOperationRequest(ConfigOperationType optType, String group, String key, String value) {
        this.optType = optType;
        this.group = group;
        this.key = key;
        this.value = value;
    }

    public static ConfigOperationRequest buildGetRequest(String group, String key) {
        return new ConfigOperationRequest(ConfigOperationType.GET, group, key);
    }

    public static ConfigOperationRequest buildPutRequest(String group, String key, String value) {
        return new ConfigOperationRequest(ConfigOperationType.PUT, group, key, value);
    }

    public static ConfigOperationRequest buildDeleteRequest(String group, String key) {
        return new ConfigOperationRequest(ConfigOperationType.DELETE, group, key);
    }

    public static ConfigOperationRequest buildGetAllRequest(String group) {
        return new ConfigOperationRequest(ConfigOperationType.GET_ALL, group);
    }

    public ConfigOperationType getOptType() {
        return optType;
    }
    public void setOptType(ConfigOperationType optType) {
        this.optType = optType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
