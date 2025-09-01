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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.param.GlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalLockVO;
import org.apache.seata.mcp.service.GlobalLockService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.utils.DateUtils;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageResult<GlobalLockVO> queryGlobalLock(NameSpaceDetail nameSpaceDetail, GlobalLockParam param) {
        // Check whether the query interval is too large
        if (param.getTimeEnd() != null && param.getTimeStart() != null) {
            if (DateUtils.judgeExceedTimeDuration(
                    param.getTimeStart(), param.getTimeEnd(), configuration.getQueryDuration())) {
                return PageResult.failure(
                        "",
                        "The query time span is not allowed to exceed the max query duration : "
                                + DateUtils.convertToHourFromTimeStamp(configuration.getQueryDuration()) + " hour");
            }
        } else if (param.getTimeStart() != null && param.getTimeEnd() == null) {
            param.setTimeEnd(param.getTimeStart() + DateUtils.ONE_DAY_TIMESTAMP);
        } else {
            param.setTimeEnd(null);
            param.setTimeStart(null);
        }
        PageResult<GlobalLockVO> result;
        String response = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/query", param, null, null);
        try {
            result = objectMapper.readValue(response, new TypeReference<PageResult<GlobalLockVO>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (result == null) {
            return PageResult.failure("", "query global lock failed");
        } else {
            return result;
        }
    }

    @Override
    public String deleteGlobalLock(NameSpaceDetail nameSpaceDetail, GlobalLockDeleteParam param) {
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/delete", param, null, null);
        if (StringUtils.isBlank(result)) {
            return "delete global lock failed";
        } else {
            return result;
        }
    }

    @Override
    public String checkGlobalLock(NameSpaceDetail nameSpaceDetail, String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/check", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("check global lock failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }
}
