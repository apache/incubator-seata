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
package org.apache.seata.mcp.tools;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.constant.RPCConstant;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BranchSessionTools {

    private final ConsoleApiService mcpRPCService;

    private final ModifyConfirmService modifyConfirmService;

    public BranchSessionTools(ConsoleApiService mcpRPCService, ModifyConfirmService modifyConfirmService) {
        this.mcpRPCService = mcpRPCService;
        this.modifyConfirmService = modifyConfirmService;
    }

    @McpTool(description = "Delete branch transactions, Get the modify key before you delete")
    public String deleteBranchSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Branch transaction id") String branchId,
            @McpToolParam(description = "Modify key") String modifyKey) {
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

    @McpTool(description = "Stop the branch transaction retry, Get the modify key before you stop")
    public String stopBranchSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Branch transaction id") String branchId,
            @McpToolParam(description = "Modify key") String modifyKey) {
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

    @McpTool(description = "Initiate a branch transaction retries, Get the modify key before you start")
    public String startBranchRetry(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Branch transaction id") String branchId,
            @McpToolParam(description = "Modify key") String modifyKey) {
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
