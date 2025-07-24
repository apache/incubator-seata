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
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.service.GlobalLockService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GlobalLockServiceImpl implements GlobalLockService {
    @Autowired
    private MCPRPCService mcpRPCService;

    @Autowired
    private MCPProperties configuration;

    @Override
    public String queryGlobalLock(GlobalLockParam param) {
        String result = mcpRPCService.getCallTC(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query", param, null, null);
        // Check whether the query interval is too large
        if (param.getTimeEnd() != null && param.getTimeStart() != null) {
            if (param.getTimeEnd() - param.getTimeStart()
                    > configuration.getQueryDuration()) {
                return "The query time span is not allowed to exceed the max query duration";
            }
        }
        if (StringUtils.isBlank(result)) {
            return "query global lock failed";
        } else {
            return result;
        }
    }

    @Override
    public String deleteGlobalLock(GlobalLockParam param) {
        String result = mcpRPCService.deleteCallTC(RPCConstant.GLOBAL_LOCK_BASE_URL + "/delete", param, null, null);
        if (StringUtils.isBlank(result)) {
            return "delete global lock failed";
        } else {
            return result;
        }
    }

    @Override
    public String checkGlobalLock(String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.getCallTC(RPCConstant.GLOBAL_LOCK_BASE_URL + "/check", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("check global lock failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }
}
