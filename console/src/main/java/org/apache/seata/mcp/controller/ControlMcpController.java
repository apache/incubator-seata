package org.apache.seata.mcp.controller;

import org.apache.seata.mcp.manager.McpServerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControlMcpController {

    @Autowired
    McpServerManager mcpServerEndpointProvider;

    @PostMapping("/open")
    public String openMcp() {
        boolean isRunning = mcpServerEndpointProvider.isRunning();
        if (!isRunning) {
            mcpServerEndpointProvider.resume();
            return "服务器启动成功";
        } else {
            return "服务器正在运行，无须启动";
        }
    }

    @PostMapping("/close")
    public String closeMcp() {
        boolean isRunning = mcpServerEndpointProvider.isRunning();
        if (!isRunning) {
            return "服务器已经关闭，无须关闭";
        } else {
            mcpServerEndpointProvider.pause();
            return "服务器关闭成功";
        }
    }
}
