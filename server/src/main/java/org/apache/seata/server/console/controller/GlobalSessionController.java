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

import javax.annotation.Resource;

import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global Session Controller
 */
@RestController
@RequestMapping("/api/v1/console/globalSession")
public class GlobalSessionController {

    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

    /**
     * Query all globalSession
     * @param param param for query globalSession
     * @return  the list of GlobalSessionVO
     */
    @GetMapping("query")
    public PageResult<GlobalSessionVO> query(@ModelAttribute GlobalSessionParam param) {
        return globalSessionService.query(param);
    }

}
