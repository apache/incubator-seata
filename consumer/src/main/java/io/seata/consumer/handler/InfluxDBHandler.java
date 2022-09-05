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
import io.seata.consumer.Constants;
import io.seata.consumer.utils.InfluxDBUtils;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

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
            GlobalSession globalSession = JSONObject.parseObject(valueString, GlobalSession.class);
            tags.put("xid", globalSession.getXid());
            tags.put("applicationId", globalSession.getApplicationId());
            tags.put("transactionServiceGroup", globalSession.getTransactionServiceGroup());
            tags.put("transactionName", globalSession.getTransactionName());
            tags.put("applicationData", globalSession.getApplicationData());

            fields.put("transactionId", globalSession.getTransactionId());
            fields.put("status", globalSession.getStatus());
            fields.put("timeout", globalSession.getTimeout());
            fields.put("beginTime", globalSession.getBeginTime());
        } else if (topic.equals(Constants.branchSessionTopic)) {
            BranchSession branchSession = JSONObject.parseObject(valueString, BranchSession.class);
            tags.put("xid", branchSession.getXid());
            tags.put("resourceGroupId", branchSession.getResourceGroupId());
            tags.put("resourceId", branchSession.getResourceId());
            tags.put("branchType", branchSession.getBranchType().name());
            tags.put("clientId", branchSession.getClientId());
            tags.put("applicationData", branchSession.getApplicationData());

            fields.put("branchId", branchSession.getBranchId());
            fields.put("transactionId", branchSession.getTransactionId());
            fields.put("status", branchSession.getStatus());
        } else if (topic.equals(Constants.undoTopic)) {
            BranchUndoLog branchUndoLog = JSONObject.parseObject(valueString, BranchUndoLog.class);
            tags.put("xid", branchUndoLog.getXid());

            fields.put("branchId", branchUndoLog.getBranchId());
            fields.put("sqlUndoLogs", branchUndoLog.getSqlUndoLogs().toString());

        } else {
            throw new IllegalArgumentException("not support topic:" + topic);
        }
        measurement = topic;
        timestamp = System.currentTimeMillis();
        InfluxDBUtils.insert(measurement, tags, fields, timestamp);
    }
}
