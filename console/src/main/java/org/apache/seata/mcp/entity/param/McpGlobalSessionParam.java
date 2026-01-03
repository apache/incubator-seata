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
package org.apache.seata.mcp.entity.param;

import org.apache.seata.common.util.PageUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.utils.DateUtils;
import org.apache.seata.mcp.entity.dto.McpGlobalSessionParamDto;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;

/**
 * Global session param
 */
public class McpGlobalSessionParam extends GlobalSessionParam {

    public static McpGlobalSessionParam convertFromDtoParam(McpGlobalSessionParamDto paramDto) {
        PageUtil.checkParam(paramDto.getPageNum(), paramDto.getPageSize());
        McpGlobalSessionParam param = new McpGlobalSessionParam();
        param.setPageSize(paramDto.getPageSize());
        param.setPageNum(paramDto.getPageNum());
        param.setStatus(paramDto.getStatus());
        param.setXid(paramDto.getXid());
        param.setApplicationId(paramDto.getApplicationId());
        param.setTransactionName(paramDto.getTransactionName());
        param.setWithBranch(paramDto.isWithBranch());
        if (StringUtils.isNotBlank(paramDto.getTimeStart())) {
            param.setTimeStart(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeStart()));
        }
        if (StringUtils.isNotBlank(paramDto.getTimeEnd())) {
            param.setTimeEnd(DateUtils.convertToTimeStampFromDateTime(paramDto.getTimeEnd()));
        }
        return param;
    }
}
