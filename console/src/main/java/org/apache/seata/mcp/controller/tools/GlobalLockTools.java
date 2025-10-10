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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.dto.GlobalLockParamDto;
import org.apache.seata.mcp.entity.param.GlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalLockVO;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.apache.seata.mcp.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GlobalLockTools {
    @Autowired
    private MCPRPCService mcpRPCService;

    @Autowired
    private MCPProperties configuration;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    @Autowired
    private ObjectMapper objectMapper;

    @Tool(description = "Query the global lock information")
    public PageResult<GlobalLockVO> queryGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global lock parameters", required = true) GlobalLockParamDto paramDto) {
        GlobalLockParam param = GlobalLockParam.convertFromParamDto(paramDto);
        if (param.getTimeStart() != null) {
            if (param.getTimeEnd() != null) {
                if (DateUtils.judgeExceedTimeDuration(
                        param.getTimeStart(), param.getTimeEnd(), configuration.getQueryDuration())) {
                    return PageResult.failure(
                            "",
                            "The query time span is not allowed to exceed the max query duration : "
                                    + DateUtils.convertToHourFromTimeStamp(configuration.getQueryDuration()) + " hour");
                }
            } else {
                param.setTimeEnd(param.getTimeStart() + DateUtils.ONE_DAY_TIMESTAMP);
            }
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

    @Tool(description = "Delete the global lock, Get the modify key before you delete")
    public String deleteGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global lock delete parameters", required = true) GlobalLockDeleteParam param,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_LOCK_BASE_URL + "/delete", param, null, null);
        if (StringUtils.isBlank(result)) {
            return "delete global lock failed";
        } else {
            return result;
        }
    }

    @Tool(description = "Check if the lock exist the branch session")
    public String checkGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Branch transaction id", required = true) String branchId) {
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
