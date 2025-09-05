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

import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.service.ServerLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/console/serverLog")
public class ServerLogController {
    @Autowired
    private ServerLogService serverLogService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLogController.class);

    @GetMapping("/getCurrentServerLogFile")
    public ResponseEntity<StreamingResponseBody> getCurrentServerLogFile(@ModelAttribute ServerLogParam param) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("User try to get current server log file, log type:{}", param.getLogType());
        }
        return serverLogService.getServerLogFile(param);
    }
}
