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

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.cluster.raft.execute.config.ConfigOperationType;
import org.apache.seata.server.cluster.raft.processor.request.ConfigOperationRequest;

import java.io.Serializable;

import static org.apache.seata.common.ConfigurationKeys.CONFIG_STORE_GROUP;
import static org.apache.seata.common.Constants.DEFAULT_STORE_GROUP;


public class ConfigOperationDTO implements Serializable {
    private static final long serialVersionUID = -1237293571963636954L;

    private ConfigOperationType optType;
    private String group = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(CONFIG_STORE_GROUP, DEFAULT_STORE_GROUP);;
    private String key;
    private Object value;

    public ConfigOperationDTO() {
    }

    public ConfigOperationDTO(ConfigOperationType optType, String group, String key, Object value) {
        this.optType = optType;
        this.group = group;
        this.key = key;
        this.value = value;
    }

    public ConfigOperationDTO(ConfigOperationType optType, String group, String key){
        this.optType = optType;
        this.group = group;
        this.key = key;
    }
    public ConfigOperationDTO(ConfigOperationType optType,  String key){
        this.optType = optType;
        this.key = key;
    }
    public ConfigOperationDTO(ConfigOperationType optType, String key, Object value) {
        this.optType = optType;
        this.key = key;
        this.value = value;
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

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static ConfigOperationDTO convertConfigRequest2Dto(ConfigOperationRequest request) {
        return new ConfigOperationDTO(request.getOptType(), request.getGroup(), request.getKey(), request.getValue());
    }

    @Override
    public String toString() {
        return "ConfigOperationDTO{" +
                "optType=" + optType +
                ", group='" + group + '\'' +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
