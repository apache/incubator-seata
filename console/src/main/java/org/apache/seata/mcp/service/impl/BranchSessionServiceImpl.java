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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.BranchSessionService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BranchSessionServiceImpl implements BranchSessionService {

    @Autowired
    @Lazy
    private MCPRPCService mcpRPCService;

    @Override
    public String deleteBranchSession(NameSpaceDetail nameSpaceDetail, String xid, String branchId) {
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

    @Override
    public String forceDeleteBranchSession(NameSpaceDetail nameSpaceDetail, String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail,
                RPCConstant.BRANCH_SESSION_BASE_URL + "/forceDeleteBranchSession",
                null,
                pathParams,
                null);
        if (StringUtils.isBlank(result)) {
            return String.format("force delete branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Override
    public String stopBranchSession(NameSpaceDetail nameSpaceDetail, String xid, String branchId) {
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

    @Override
    public String startBranchRetry(NameSpaceDetail nameSpaceDetail, String xid, String branchId) {
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
