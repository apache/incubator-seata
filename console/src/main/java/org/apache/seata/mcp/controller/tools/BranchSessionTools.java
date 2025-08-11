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
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.BranchSessionService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BranchSessionTools {

    @Autowired
    private BranchSessionService branchSessionService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSessionTools.class);

    @Tool(description = "Delete branch transactions, Get the modify key before you delete")
    public String deleteBranchSession(
            @ToolParam(description = "Specify the namespace of the TC node",required = true) NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id",required = true) String xid,
            @ToolParam(description = "Branch transaction id",required = true) String branchId,
            @ToolParam(description = "Modify key",required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.deleteBranchSession(nameSpaceDetail, xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Force the deletion of branch transactions, Get the modify key before you delete")
    public String forceDeleteBranchSession(
            @ToolParam(description = "Specify the namespace of the TC node",required = true) NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id",required = true) String xid,
            @ToolParam(description = "Branch transaction id",required = true) String branchId,
            @ToolParam(description = "Modify key",required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to force delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.forceDeleteBranchSession(nameSpaceDetail, xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Stop the branch transaction retry, Get the modify key before you stop")
    public String stopBranchSession(
            @ToolParam(description = "Specify the namespace of the TC node",required = true) NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id",required = true) String xid,
            @ToolParam(description = "Branch transaction id",required = true) String branchId,
            @ToolParam(description = "Modify key",required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.stopBranchSession(nameSpaceDetail, xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Initiate a branch transaction retries, Get the modify key before you start")
    public String startBranchRetry(
            @ToolParam(description = "Specify the namespace of the TC node",required = true) NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id",required = true) String xid,
            @ToolParam(description = "Branch transaction id",required = true) String branchId,
            @ToolParam(description = "Modify key",required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.startBranchRetry(nameSpaceDetail, xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }
}
