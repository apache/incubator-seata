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
package org.apache.seata.server.cluster.raft.execute.config;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.server.cluster.listener.ClusterConfigChangeEvent;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftConfigOperationSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.dto.ConfigOperationDTO;
import org.apache.seata.server.config.ConfigurationItem;
import org.apache.seata.server.config.ConfigurationProcessor;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;


public class ConfigOperationExecute extends AbstractRaftConfigMsgExecute {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigOperationExecute.class);

    @Override
    public Object execute(RaftBaseMsg syncMsg) throws Throwable {
        RaftConfigOperationSyncMsg configSyncMsg = (RaftConfigOperationSyncMsg) syncMsg;
        ConfigOperationDTO configOperation = configSyncMsg.getConfigOperation();
        switch (configOperation.getOptType()) {
            case PUT:
                return put(configOperation);
            case DELETE:
                return delete(configOperation);
            case DELETE_ALL:
                return deleteAll(configOperation);
            case GET:
                return get(configOperation);
            case GET_ALL:
                return getAll(configOperation);
            case UPLOAD:
                return upload(configOperation);
            case GET_NAMESPACES:
                return getNamespaces(configOperation);
            case GET_DATA_IDS:
                return getDataIds(configOperation);
            default:
                return ConfigOperationResponse.fail("unknown operation type");
        }
    }

    private ConfigOperationResponse get(ConfigOperationDTO configOperation) {
        String result = configStoreManager.get(configOperation.getNamespace(), configOperation.getDataId(), configOperation.getKey());
        // fill config description and default value
        ConfigurationItem item = ConfigurationProcessor.processConfigItem(configOperation.getKey(), result);
        return ConfigOperationResponse.success(item);
    }

    private ConfigOperationResponse put(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.put(configOperation.getNamespace(), configOperation.getDataId(), configOperation.getKey(), configOperation.getValue());
        if (success) {
            // ApplicationContext may not have been started at this point
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getNamespace(), configOperation.getDataId()));
            }
            LOGGER.info("config namespace: {}, dataId: {}, config change event: {}", configOperation.getNamespace(), configOperation.getDataId(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse delete(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.delete(configOperation.getNamespace(), configOperation.getDataId(), configOperation.getKey());
        if (success) {
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getNamespace(), configOperation.getDataId()));
            }
            LOGGER.info("config namespace: {}, dataId: {}, config change event: {}", configOperation.getNamespace(), configOperation.getDataId(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse deleteAll(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.deleteAll(configOperation.getNamespace(), configOperation.getDataId());
        if (success) {
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getNamespace(), configOperation.getDataId()));
            }
            LOGGER.info("config namespace: {}, dataId: {}, config change event: {}", configOperation.getNamespace(), configOperation.getDataId(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse upload(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.putAll(configOperation.getNamespace(), configOperation.getDataId(), (Map<String, Object>) configOperation.getValue());
        if (success) {
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getNamespace(), configOperation.getDataId()));
            }
            LOGGER.info("config namespace: {}, dataId: {}, config change event: {}", configOperation.getNamespace(), configOperation.getDataId(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse getAll(ConfigOperationDTO configOperation) {
        Map<String, Object> configMap = configStoreManager.getAll(configOperation.getNamespace(), configOperation.getDataId());
        Long configVersion = configStoreManager.getConfigVersion(configOperation.getNamespace(), configOperation.getDataId());
        Map<String, Object> result = new HashMap<>();
        // fill config description and default value
        Map<String, ConfigurationItem> itemMap = ConfigurationProcessor.processConfigMap(configMap);
        result.put("config", itemMap);
        result.put("version", configVersion);
        return ConfigOperationResponse.success(result);
    }

    private ConfigOperationResponse getNamespaces(ConfigOperationDTO configOperation){
        List<String> namespaces = configStoreManager.getAllNamespaces();
        return ConfigOperationResponse.success(namespaces);
    }

    private ConfigOperationResponse getDataIds(ConfigOperationDTO configOperation){
        List<String> dataIds = configStoreManager.getAllDataIds(configOperation.getNamespace());
        return ConfigOperationResponse.success(dataIds);
    }
}
