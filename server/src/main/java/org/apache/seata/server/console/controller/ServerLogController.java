package org.apache.seata.server.console.controller;

import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.service.ServerLogService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/console/serverLog")
public class ServerLogController {
    @Autowired
    private ServerLogService serverLogService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLogController.class);

    @GetMapping("/getServerLog")
    public String getServerLog(@ModelAttribute ServerLogParam serverLogParam) {
        return serverLogService.getServerLog(serverLogParam);
    }
}
