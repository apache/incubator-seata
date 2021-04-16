/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.controller;

import io.seata.server.ServerRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author spilledyear@outlook.com
 */
@Controller
@RequestMapping
public class HealthController {

    private static final String OK = "ok";
    private static final String NOT_OK = "not_ok";

    @Autowired
    private ServerRunner serverRunner;


    @RequestMapping("/health")
    @ResponseBody
    String healthCheck() {
        return serverRunner.started() ? OK : NOT_OK;
    }
}