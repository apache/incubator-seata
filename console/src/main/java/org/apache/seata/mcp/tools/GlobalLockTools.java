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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.constant.RPCConstant;
import org.apache.seata.mcp.core.props.MCPProperties;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.core.utils.DateUtils;
import org.apache.seata.mcp.entity.dto.McpGlobalLockParamDto;
import org.apache.seata.mcp.entity.param.McpGlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.McpGlobalLockParam;
import org.apache.seata.mcp.entity.vo.McpGlobalLockVO;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GlobalLockTools {

    private final Logger logger = LoggerFactory.getLogger(GlobalLockTools.class);

    private final ConsoleApiService mcpRPCService;

    private final MCPProperties mcpProperties;

    private final ModifyConfirmService modifyConfirmService;

    private final ObjectMapper objectMapper;

    public GlobalLockTools(
            ConsoleApiService mcpRPCService,
            MCPProperties mcpProperties,
            ModifyConfirmService modifyConfirmService,
            ObjectMapper objectMapper) {
        this.mcpRPCService = mcpRPCService;
        this.mcpProperties = mcpProperties;
        this.modifyConfirmService = modifyConfirmService;
        this.objectMapper = objectMapper;
    }

    @McpTool(description = "Query the global lock information")
    public PageResult<McpGlobalLockVO> queryGlobalLock(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global lock parameters") McpGlobalLockParamDto paramDto) {
        McpGlobalLockParam param = McpGlobalLockParam.convertFromParamDto(paramDto);
        Long timeStart = param.getTimeStart();
        Long timeEnd = param.getTimeEnd();
        Long maxQueryDuration = mcpProperties.getQueryDuration();
        if (timeStart != null || timeEnd != null) {
            if (timeStart == null) {
                timeStart = timeEnd - maxQueryDuration;
                param.setTimeStart(timeStart);
            }
            if (timeEnd == null) {
                timeEnd = timeStart + maxQueryDuration;
                param.setTimeEnd(timeEnd);
            }
            if (DateUtils.judgeExceedTimeDuration(timeStart, timeEnd, maxQueryDuration)) {
                return PageResult.failure(
                        "",
                        String.format(
                                "The query time span is not allowed to exceed the max query duration: %s hours",
                                DateUtils.convertToHourFromTimeStamp(maxQueryDuration)));
            }
        }
        PageResult<McpGlobalLockVO> result = null;
        String response = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/query", param, null, null);
        try {
            result = objectMapper.readValue(response, new TypeReference<PageResult<McpGlobalLockVO>>() {});
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        if (result == null) {
            return PageResult.failure("", "query global lock failed");
        } else {
            return result;
        }
    }

    @McpTool(description = "Delete the global lock, Get the modify key before you delete")
    public String deleteGlobalLock(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global lock delete parameters") McpGlobalLockDeleteParam param,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/delete", param, null, null);
        if (StringUtils.isBlank(result)) {
            return String.format(
                    "delete global lock failed, xid: %s, branchId: %s", param.getXid(), param.getBranchId());
        } else {
            return result;
        }
    }

    @McpTool(description = "Check if the branch session has a lock")
    public String checkGlobalLock(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Branch transaction id") String branchId) {
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
