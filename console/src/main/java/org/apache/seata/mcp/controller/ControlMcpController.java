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
package org.apache.seata.mcp.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.mcp.manager.MCPServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/mcp/service")
public class ControlMcpController {

    @Autowired
    MCPServerManager mcpServerEndpointProvider;

    @PutMapping("/changeStatus")
    public SingleResult<?> changeStatus(@RequestParam(defaultValue = "") String status) {
        switch (status.toLowerCase(Locale.ROOT)) {
            case "start":
                return startMcpService();
            case "stop":
                return stopMcpService();
            default:
                return SingleResult.failure("Only the start and stop status are supported");
        }
    }

    @GetMapping("/getStatus")
    public SingleResult<?> getStatus() {
        boolean isRunning = mcpServerEndpointProvider.isRunning();
        if (isRunning) {
            return SingleResult.success("MCP Service is Running");
        } else {
            return SingleResult.success("MCP Service is Stopped");
        }
    }

    public SingleResult<?> startMcpService() {
        boolean isRunning = mcpServerEndpointProvider.isRunning();
        if (!isRunning) {
            mcpServerEndpointProvider.resume();
            return SingleResult.success("The server started successfully");
        } else {
            return SingleResult.failure("The server is running and does not need to be started");
        }
    }

    public SingleResult<?> stopMcpService() {
        boolean isRunning = mcpServerEndpointProvider.isRunning();
        if (!isRunning) {
            return SingleResult.failure("The server is down and does not need to be shut down");
        } else {
            mcpServerEndpointProvider.pause();
            return SingleResult.success("The server stopped successfully");
        }
    }
}
