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
package io.seata.server.console.controller;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Global Session Controller
 * @author: zhongxiang.wang
 */
@RestController
@RequestMapping("console/globalSession")
public class GlobalSessionController {

    @Resource(type = GlobalSessionService.class)
    private GlobalSessionService globalSessionService;

    /**
     * Query all globalSession
     * @param param
     * @return  the list of GlobalSessionVO
     */
    @GetMapping("query")
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        throw new NotSupportYetException();
    }
}
