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

import javax.annotation.Resource;

import io.seata.console.result.PageResult;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.vo.GlobalLockVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global Lock Controller
 *
 * @author zhongxiang.wang
 */
@RestController
@RequestMapping("/api/v1/console/globalLock")
public class GlobalLockController {

    @Resource(type = GlobalLockService.class)
    private GlobalLockService globalLockService;

    /**
     * Query locks by param
     *
     * @param param the param
     * @return the list of GlobalLockVO
     */
    @GetMapping("query")
    public PageResult<GlobalLockVO> query(@ModelAttribute GlobalLockParam param) {
        return globalLockService.query(param);
    }

}
