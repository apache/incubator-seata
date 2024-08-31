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
package org.apache.seata.server.console.controller;

import com.alibaba.fastjson.JSON;
import org.apache.seata.core.rpc.netty.http.HttpController;
import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.seata.server.console.param.ParamUtil.*;

@Component
public class GlobalSessionController implements HttpController {
    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

    @Override
    public Set<String> getPath() {
        return new HashSet<String>() {{
            add("/api/v1/console/globalSession");
        }};
    }

    @Override
    public String handle(String path, Map<String, List<String>> paramMap) {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setXid(getStringParam(paramMap, "xid"));
        param.setApplicationId(getStringParam(paramMap, "applicationId"));
        param.setStatus(getIntParam(paramMap, "status"));
        param.setTransactionName(getStringParam(paramMap, "transactionName"));
        param.setWithBranch(getBooleanParam(paramMap, "withBranch"));
        param.setPageSize(getIntParam(paramMap, "pageSize"));
        param.setPageNum(getIntParam(paramMap, "pageNum"));
        param.setTimeEnd(getLongParam(paramMap, "timeEnd"));
        param.setTimeStart(getLongParam(paramMap, "timeStart"));

        return JSON.toJSONString(globalSessionService.query(param));
    }
}
