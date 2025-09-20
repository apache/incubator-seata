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
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.ServerLogPageVO;
import org.apache.seata.mcp.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerLogTools {

    @Autowired
    private ServerLogService logService;

    @Tool(
            description =
                    "This tool is used to fetch log data. The response includes a field `hasMorePages`:\n"
                            + "- When `hasMorePages = true`, it means there are more pages to fetch, and you must continue calling this tool.\n"
                            + "- For the next page, increment the parameter `pageNum` by +1 from the previous call.\n"
                            + "- Continue calling this tool until a response with `hasMorePages = false` is received.\n"
                            + "- All logs from all pages should be combined into a single list in chronological order. You don’t need to handle pagination logic yourself—just merge the results.\n")
    public ServerLogPageVO<String> getServerLogFile(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(
                            description = "server log file query parameters(If possible, avoid using full queries)",
                            required = true)
                    ServerLogParam param) {
        return logService.analyseServerLogFile(nameSpaceDetail, param);
    }
}
