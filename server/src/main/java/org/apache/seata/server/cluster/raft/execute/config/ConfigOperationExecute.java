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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

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
            case GET:
                return get(configOperation);
            case GET_ALL:
                return getAll(configOperation);
            default:
                return ConfigOperationResponse.fail("unknown operation type");
        }
    }

    private ConfigOperationResponse get(ConfigOperationDTO configOperation) {
        String result = configStoreManager.get(configOperation.getGroup(), configOperation.getKey());
        return ConfigOperationResponse.success(result);
    }

    private ConfigOperationResponse put(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.put(configOperation.getGroup(), configOperation.getKey(), configOperation.getValue());
        if (success) {
            // ApplicationContext may not have been started at this point
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getGroup()));
            }
            LOGGER.info("config group: {}, config change event: {}", configOperation.getGroup(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse delete(ConfigOperationDTO configOperation) {
        Boolean success = configStoreManager.delete(configOperation.getGroup(), configOperation.getKey());
        if (success) {
            if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT) != null){
                ((ApplicationEventPublisher) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                        .publishEvent(new ClusterConfigChangeEvent(this, configOperation.getGroup()));
            }
            LOGGER.info("config group: {}, config change event: {}", configOperation.getGroup(), configOperation.getOptType());
        }
        return success? ConfigOperationResponse.success() : ConfigOperationResponse.fail();
    }

    private ConfigOperationResponse getAll(ConfigOperationDTO configOperation) {
        Map<String, Object> configMap = configStoreManager.getAll(configOperation.getGroup());
        return ConfigOperationResponse.success(configMap);
    }
}
