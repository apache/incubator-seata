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
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.dto.GlobalSessionParamDto;
import org.apache.seata.mcp.entity.param.GlobalAbnormalSessionParam;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalSessionVO;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.apache.seata.mcp.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GlobalSessionTools {

    @Autowired
    private MCPRPCService mcpRPCService;

    @Autowired
    private MCPProperties configuration;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private final List<Integer> exceptionStatus = new ArrayList<>();

    @Tool(description = "Query global transactions")
    public PageResult<GlobalSessionVO> queryGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Query parameter objects", required = true) GlobalSessionParamDto paramDto) {
        GlobalSessionParam param = GlobalSessionParam.covertFromDtoParam(paramDto);
        if (param.getTimeStart() != null) {
            if (param.getTimeEnd() != null) {
                if (DateUtils.judgeExceedTimeDuration(
                        param.getTimeStart(), param.getTimeEnd(), configuration.getQueryDuration())) {
                    throw new IllegalArgumentException(
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
        PageResult<GlobalSessionVO> pageResult;
        String result = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/query", param, null, null);
        try {
            pageResult = objectMapper.readValue(result, new TypeReference<PageResult<GlobalSessionVO>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (pageResult == null) {
            return PageResult.failure("", "query global session failed");
        } else {
            return pageResult;
        }
    }

    @Tool(description = "Delete the global session, Get the modify key before you delete")
    public String deleteGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
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

    @Tool(description = "Stop the global session retry, Get the modify key before you stop")
    public String stopGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
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

    @Tool(description = "Start the global session retry, Get the modify key before you start")
    public String startGlobalSession(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
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

    @Tool(description = "Send global session to commit or rollback to rm, Get the modify key before you send")
    public String sendCommitOrRollback(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
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

    @Tool(
            description =
                    "Change the global session status, Used to change transactions that are in a failed commit or rollback failed state to a retry state, Get the modify key before you change")
    public String changeGlobalStatus(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
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

    @Tool(description = "Check out the abnormal transaction information,You can specify the time")
    public List<GlobalSessionVO> getAbnormalSessions(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Query Param", required = true) GlobalAbnormalSessionParam abnormalSessionParam) {
        List<GlobalSessionVO> result = new ArrayList<>();
        GlobalSessionParamDto param = GlobalSessionParamDto.covertFromAbnormalParam(abnormalSessionParam);
        param.setPageNum(1);
        param.setPageSize(100);
        if (exceptionStatus.isEmpty()) {
            exceptionStatus.add(GlobalStatus.CommitFailed.getCode());
            exceptionStatus.add(GlobalStatus.TimeoutRollbackFailed.getCode());
            exceptionStatus.add(GlobalStatus.RollbackFailed.getCode());
        }
        for (Integer status : exceptionStatus) {
            param.setStatus(status);
            List<GlobalSessionVO> datas =
                    queryGlobalSession(nameSpaceDetail, param).getData();
            if (datas != null && !datas.isEmpty()) {
                for (GlobalSessionVO vo : datas) {
                    if (result.size() >= 200) {
                        return result;
                    }
                    result.add(vo);
                }
            }
        }
        return result;
    }
}
