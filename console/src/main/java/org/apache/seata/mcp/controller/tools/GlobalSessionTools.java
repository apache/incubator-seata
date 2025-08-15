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
package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.GlobalSessionService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use RPC to call the server-side corresponding method
 */
@Service
public class GlobalSessionTools {

    @Autowired
    private GlobalSessionService globalSessionService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionTools.class);

    @Tool(description = "Check out the abnormal transaction information,You can specify the time")
    public List<String> getAbnormalTransactionInfo(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Millisecond timestamps,Long string") String startTime,
            @ToolParam(description = "Millisecond timestamps,Long string") String endTime) {
        Long start = null;
        Long end = null;
        if (startTime != null && endTime != null) {
            start = Long.parseLong(startTime);
            end = Long.parseLong(endTime);
        }
        return globalSessionService.getAbnormalSessions(nameSpaceDetail, start, end);
    }

    @Tool(description = "Query global transactions")
    public String queryGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Query parameter objects", required = true) GlobalSessionParam param) {
        return globalSessionService.queryGlobalSession(nameSpaceDetail, param);
    }

    @Tool(description = "Delete the global session, Get the modify key before you delete")
    public String deleteGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.deleteGlobalSession(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Force Delete the global session, Get the modify key before you delete")
    public String forceDeleteGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to force delete the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.forceDeleteGlobalSession(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Stop the global session retry, Get the modify key before you stop")
    public String stopGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.stopGlobalSession(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Start the global session retry, Get the modify key before you start")
    public String startGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.startGlobalSession(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Send global session to commit or rollback to rm, Get the modify key before you send")
    public String sendCommitOrRollback(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to commit or rollback the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.sendCommitOrRollback(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Change the global session status, Get the modify key before you change")
    public String changeGlobalStatus(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to change the global session, xid: {}", xid);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalSessionService.changeGlobalStatus(nameSpaceDetail, xid);
        } else {
            return "the modify key is not available";
        }
    }
}
