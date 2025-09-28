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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BranchSessionTools {

    @Autowired
    private MCPRPCService mcpRPCService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    @Tool(description = "Delete branch transactions, Get the modify key before you delete")
    public String deleteBranchSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Branch transaction id", required = true) String branchId,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail, RPCConstant.BRANCH_SESSION_BASE_URL + "/deleteBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("delete branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Tool(description = "Stop the branch transaction retry, Get the modify key before you stop")
    public String stopBranchSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Branch transaction id", required = true) String branchId,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.BRANCH_SESSION_BASE_URL + "/stopBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("stop branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Tool(description = "Initiate a branch transaction retries, Get the modify key before you start")
    public String startBranchRetry(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Branch transaction id", required = true) String branchId,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.BRANCH_SESSION_BASE_URL + "/startBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("start branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }
}
