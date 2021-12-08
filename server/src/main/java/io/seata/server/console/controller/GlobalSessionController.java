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

import io.seata.core.store.db.vo.GlobalSessionVO;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.result.SingleResult;
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
     * @return
     */
    @GetMapping("queryAll")
    public PageResult<GlobalSessionVO> queryAll(String applicationId, boolean withBranch) {
        return globalSessionService.queryAll(applicationId, withBranch);
    }

    /**
     * Query all globalSession by status
     * @param applicationId
     * @param status
     * @param withBranch
     * @return
     */
    @GetMapping("queryByStatus")
    public PageResult<GlobalSessionVO> queryByStatus(String applicationId, Integer status, boolean withBranch) {
        return globalSessionService.queryByStatus(applicationId, status, withBranch);
    }

    @GetMapping("queryByXid")
    SingleResult<GlobalSessionVO> queryByXid(String xid, boolean withBranch) {
        return globalSessionService.queryByXid(xid, withBranch);
    }
}
