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

import io.seata.core.store.db.vo.BranchSessionVO;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.service.BranchSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Branch Session Controller
 * @author: zhongxiang.wang
 */
@RestController
@RequestMapping("seata/console/branchSession")
public class BranchSessionController {

    @Resource(type = BranchSessionService.class)
    private BranchSessionService branchSessionService;

    /**
     * Query all branchSession
     * @return
     */
    @GetMapping("queryByXid")
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        return branchSessionService.queryByXid(xid);
    }

}
