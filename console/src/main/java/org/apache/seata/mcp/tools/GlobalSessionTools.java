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
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.core.constant.RPCConstant;
import org.apache.seata.mcp.core.props.MCPProperties;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.core.utils.DateUtils;
import org.apache.seata.mcp.entity.dto.McpGlobalSessionParamDto;
import org.apache.seata.mcp.entity.param.McpGlobalAbnormalSessionParam;
import org.apache.seata.mcp.entity.param.McpGlobalSessionParam;
import org.apache.seata.mcp.entity.vo.McpGlobalSessionVO;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalSessionTools {

    private final Logger logger = LoggerFactory.getLogger(GlobalSessionTools.class);

    private final ConsoleApiService mcpRPCService;

    private final MCPProperties mcpProperties;

    private final ObjectMapper objectMapper;

    private final ModifyConfirmService modifyConfirmService;

    private final List<Integer> exceptionStatus = new ArrayList<>();

    public static final int ABNORMAL_SESSION_PAGE_SIZE = 30;

    public GlobalSessionTools(
            ConsoleApiService mcpRPCService,
            MCPProperties mcpProperties,
            ObjectMapper objectMapper,
            ModifyConfirmService modifyConfirmService) {
        this.mcpRPCService = mcpRPCService;
        this.mcpProperties = mcpProperties;
        this.objectMapper = objectMapper;
        this.modifyConfirmService = modifyConfirmService;
        exceptionStatus.add(GlobalStatus.CommitFailed.getCode());
        exceptionStatus.add(GlobalStatus.TimeoutRollbackFailed.getCode());
        exceptionStatus.add(GlobalStatus.RollbackFailed.getCode());
    }

    @McpTool(description = "Query global transactions")
    public PageResult<McpGlobalSessionVO> queryGlobalSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Query parameter objects") McpGlobalSessionParamDto paramDto) {
        McpGlobalSessionParam param = McpGlobalSessionParam.convertFromDtoParam(paramDto);
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
        PageResult<McpGlobalSessionVO> pageResult = null;
        String result = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/query", param, null, null);
        try {
            pageResult = objectMapper.readValue(result, new TypeReference<PageResult<McpGlobalSessionVO>>() {});
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
        }
        if (pageResult == null) {
            return PageResult.failure("", "query global session failed");
        } else {
            return pageResult;
        }
    }

    @McpTool(description = "Delete the global session, Get the modify key before you delete")
    public String deleteGlobalSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/deleteGlobalSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("delete global session failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @McpTool(description = "Stop the global session retry, Get the modify key before you stop")
    public String stopGlobalSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/stopGlobalSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("stop global session retry failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @McpTool(description = "Start the global session retry, Get the modify key before you start")
    public String startGlobalSession(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/startGlobalSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("start the global session retry failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @McpTool(description = "Send global session to commit or rollback to rm, Get the modify key before you send")
    public String sendCommitOrRollback(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/sendCommitOrRollback", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("send global session to commit or rollback to rm failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @McpTool(
            description =
                    "Change the global session status, Used to change transactions that are in a failed commit or rollback failed state to a retry state, Get the modify key before you change")
    public String changeGlobalStatus(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Global transaction id") String xid,
            @McpToolParam(description = "Modify key") String modifyKey) {
        if (!modifyConfirmService.isValidKey(modifyKey)) {
            return "The modify key is not available";
        }
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.putCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/changeGlobalStatus", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("change the global session status failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @McpTool(description = "Check out the abnormal transaction")
    public List<McpGlobalSessionVO> getAbnormalSessions(
            @McpToolParam(description = "Specify the namespace of the TC node") NameSpaceDetail nameSpaceDetail,
            @McpToolParam(description = "Query Param") McpGlobalAbnormalSessionParam abnormalSessionParam) {
        List<McpGlobalSessionVO> result = new ArrayList<>();
        McpGlobalSessionParamDto param = McpGlobalSessionParamDto.convertFromAbnormalParam(abnormalSessionParam);
        param.setPageSize(ABNORMAL_SESSION_PAGE_SIZE);
        for (Integer status : exceptionStatus) {
            param.setStatus(status);
            List<McpGlobalSessionVO> datas =
                    queryGlobalSession(nameSpaceDetail, param).getData();
            if (datas != null && !datas.isEmpty()) {
                result.addAll(datas);
            }
        }
        return result;
    }
}
