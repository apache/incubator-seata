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

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.ServerLogIndexParam;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerLogTools {

    @Autowired
    private ServerLogService logService;

    @Tool(description = "Get the latest or history running logs on the server side")
    public String getServerLog(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(
                            description =
                                    "server log file query parameters. when getting history logs, logTime is required",
                            required = true)
                    ServerLogParam param) {
        return logService.analyseServerLog(nameSpaceDetail, param);
    }

    @Tool(
            description =
                    "Obtain the server run log index nums of the specified type or creation time, If you do not specify a type, it is an all-type log，logTime is required")
    public String getHistoryServerLogNums(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Log file index query parameters", required = true) ServerLogIndexParam param) {
        return logService.getHistoryServerLogNums(nameSpaceDetail, param);
    }
}
