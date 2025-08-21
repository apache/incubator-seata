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
import org.apache.seata.mcp.entity.enums.GlobalExceptionStatus;
import org.apache.seata.mcp.entity.param.GlobalAbnormalSessionParam;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalSessionVO;
import org.apache.seata.mcp.service.GlobalSessionService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GlobalSessionServiceImpl implements GlobalSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServiceImpl.class);

    @Autowired
    private MCPRPCService mcpRPCService;

    @Autowired
    private MCPProperties configuration;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageResult<GlobalSessionVO> queryGlobalSession(NameSpaceDetail nameSpaceDetail, GlobalSessionParam param) {
        // Check whether the query interval is too large
        if (param.getTimeEnd() != null && param.getTimeStart() != null) {
            if (DateUtils.judgeExceedTimeDuration(param.getTimeStart(),param.getTimeEnd(),configuration.getQueryDuration())) {
                PageResult.failure("","The query time span is not allowed to exceed the max query duration(milliseconds): "
                        + configuration.getQueryDuration());
            }
        }
        PageResult<GlobalSessionVO> pageResult = new PageResult<>();
        String result = mcpRPCService.getCallTC(
                nameSpaceDetail, RPCConstant.GLOBAL_SESSION_BASE_URL + "/query", param, null, null);
        try {
            pageResult = objectMapper.readValue(result, new TypeReference<PageResult<GlobalSessionVO>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (pageResult==null) {
            return PageResult.failure("","query global session failed");
        } else {
            return pageResult;
        }
    }

    @Override
    public String deleteGlobalSession(NameSpaceDetail nameSpaceDetail, String xid) {
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

    @Override
    public String forceDeleteGlobalSession(NameSpaceDetail nameSpaceDetail, String xid) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        String result = mcpRPCService.deleteCallTC(
                nameSpaceDetail,
                RPCConstant.GLOBAL_SESSION_BASE_URL + "/forceDeleteGlobalSession",
                null,
                pathParams,
                null);
        if (StringUtils.isBlank(result)) {
            return String.format("force delete global session failed, xid: %s", xid);
        } else {
            return result;
        }
    }

    @Override
    public String stopGlobalSession(NameSpaceDetail nameSpaceDetail, String xid) {
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

    @Override
    public String startGlobalSession(NameSpaceDetail nameSpaceDetail, String xid) {
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

    @Override
    public String sendCommitOrRollback(NameSpaceDetail nameSpaceDetail, String xid) {
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

    @Override
    public String changeGlobalStatus(NameSpaceDetail nameSpaceDetail, String xid) {
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

    @Override
    public List<String> getAbnormalSessions(NameSpaceDetail nameSpaceDetail, GlobalAbnormalSessionParam abnormalSessionParam) {
        List<String> result = new ArrayList<>();
        GlobalSessionParam param = GlobalSessionParam.covertFromAbnormalParam(abnormalSessionParam);
        // Check whether the query interval is too large
        if (param.getTimeEnd() != null && param.getTimeStart() != null) {
            if (DateUtils.judgeExceedTimeDuration(param.getTimeStart(),param.getTimeEnd(),configuration.getQueryDuration())) {
                return Collections.singletonList(
                        "The query time span is not allowed to exceed the max query duration(milliseconds): "
                                + configuration.getQueryDuration());
            }
        }
        param.setPageNum(1);
        param.setPageSize(100);
        List<Integer> exceptionStatus = GlobalExceptionStatus.getAll();
        for (Integer status : exceptionStatus) {
            param.setStatus(status);
            List<GlobalSessionVO> datas = queryGlobalSession(nameSpaceDetail, param).getData();
            for(Object vo : datas){
                if(result.size()>=200){
                    return result;
                }
                result.add(vo.toString());
            }
        }
        return result;
    }
}
