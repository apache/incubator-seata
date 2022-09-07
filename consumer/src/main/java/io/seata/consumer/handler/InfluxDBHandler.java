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
package io.seata.consumer.handler;

import com.alibaba.fastjson.JSONObject;
import io.seata.common.dto.mq.BranchSessionDTO;
import io.seata.common.dto.mq.BranchUndoLogDTO;
import io.seata.common.dto.mq.GlobalSessionDTO;
import io.seata.consumer.Constants;
import io.seata.consumer.utils.InfluxDBUtils;

import java.util.HashMap;
import java.util.Map;

public class InfluxDBHandler implements Handler {
    @Override
    public void handle(String topic, byte[] key, byte[] value) {
        String measurement;
        Map<String, String> tags = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        long timestamp;
        String valueString = new String(value);
        if (topic.equals(Constants.globalSessionTopic)) {
            GlobalSessionDTO globalSessionDTO = JSONObject.parseObject(valueString, GlobalSessionDTO.class);
            tags.put("xid", globalSessionDTO.getXid());
            tags.put("applicationId", globalSessionDTO.getApplicationId());
            tags.put("transactionServiceGroup", globalSessionDTO.getTransactionServiceGroup());
            tags.put("transactionName", globalSessionDTO.getTransactionName());
            tags.put("applicationData", globalSessionDTO.getApplicationData());

            fields.put("transactionId", globalSessionDTO.getTransactionId());
            fields.put("status", globalSessionDTO.getStatus());
            fields.put("timeout", globalSessionDTO.getTimeout());
            fields.put("beginTime", globalSessionDTO.getBeginTime());
        } else if (topic.equals(Constants.branchSessionTopic)) {
            BranchSessionDTO branchSessionDTO = JSONObject.parseObject(valueString, BranchSessionDTO.class);
            tags.put("xid", branchSessionDTO.getXid());
            tags.put("resourceGroupId", branchSessionDTO.getResourceGroupId());
            tags.put("resourceId", branchSessionDTO.getResourceId());
            tags.put("branchType", branchSessionDTO.getBranchType());
            tags.put("clientId", branchSessionDTO.getClientId());
            tags.put("applicationData", branchSessionDTO.getApplicationData());

            fields.put("branchId", branchSessionDTO.getBranchId());
            fields.put("transactionId", branchSessionDTO.getTransactionId());
            fields.put("status", branchSessionDTO.getStatus());
        } else if (topic.equals(Constants.undoTopic)) {
            BranchUndoLogDTO branchUndoLogDTO = JSONObject.parseObject(valueString, BranchUndoLogDTO.class);
            tags.put("xid", branchUndoLogDTO.getXid());

            fields.put("branchId", branchUndoLogDTO.getBranchId());
            fields.put("sqlUndoLogs", branchUndoLogDTO.getSqlUndoLogs());

        } else {
            throw new IllegalArgumentException("not support topic:" + topic);
        }
        measurement = topic;
        timestamp = System.currentTimeMillis();
        InfluxDBUtils.insert(measurement, tags, fields, timestamp);
    }
}
