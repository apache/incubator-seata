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
package io.seata.server.storage.tsdb;

import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.tsdb.api.Event;
import io.seata.server.storage.tsdb.api.EventTopic;
import io.seata.server.storage.tsdb.api.UnDoEvent;
import io.seata.server.storage.tsdb.influxdb.InfluxDBUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Handler {

    public void handle(ArrayList<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        EventTopic eventTopic = events.get(0).topic;
        String measurement;
        switch (eventTopic) {
            case GLOBAL_SESSION:
                measurement = "global_session";
                break;
            case BRANCH_SESSION:
                measurement = "branch_session";
                break;
            case UNDO:
                measurement = "undo";
                break;
            default:
                throw new IllegalArgumentException();
        }
        List<Map<String, String>> tagList = new ArrayList<>();
        List<Map<String, Object>> fieldList = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        for (Event event : events) {
            Map<String, String> tags = new HashMap<>();
            Map<String, Object> fields = new HashMap<>();

            switch (event.topic) {
                case GLOBAL_SESSION:
                    GlobalSession globalSession = (GlobalSession) event.data;
                    tags.put("xid", globalSession.getXid());
                    tags.put("applicationId", globalSession.getApplicationId());
                    tags.put("transactionServiceGroup", globalSession.getTransactionServiceGroup());
                    tags.put("transactionName", globalSession.getTransactionName());
                    tags.put("applicationData", globalSession.getApplicationData());

                    fields.put("transactionId", globalSession.getTransactionId());
                    fields.put("status", globalSession.getStatus());
                    fields.put("timeout", globalSession.getTimeout());
                    fields.put("beginTime", globalSession.getBeginTime());
                    break;
                case BRANCH_SESSION:
                    BranchSession branchSession = (BranchSession) event.data;
                    tags.put("xid", branchSession.getXid());
                    tags.put("resourceGroupId", branchSession.getResourceGroupId());
                    tags.put("resourceId", branchSession.getResourceId());
                    tags.put("branchType", branchSession.getBranchType().name());
                    tags.put("clientId", branchSession.getClientId());
                    tags.put("applicationData", branchSession.getApplicationData());

                    fields.put("branchId", branchSession.getBranchId());
                    fields.put("transactionId", branchSession.getTransactionId());
                    fields.put("status", branchSession.getStatus());
                    break;
                case UNDO:
                    UnDoEvent unDoEvent = (UnDoEvent) event.data;
                    tags.put("xid", unDoEvent.getXid());
                    tags.put("rollbackCtx", unDoEvent.getRollbackCtx());

                    fields.put("branchId", unDoEvent.getBranchId());
                    fields.put("undoLogContent", unDoEvent.getUndoLogContent());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            tagList.add(tags);
            fieldList.add(fields);
            timestamps.add(event.timestamp);
        }
        InfluxDBUtils.batchInsert(measurement, tagList, fieldList, timestamps);
    }

}
