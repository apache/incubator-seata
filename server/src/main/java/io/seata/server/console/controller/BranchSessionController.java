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

import io.seata.console.result.SingleResult;
import io.seata.server.console.service.BranchSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Branch Session Controller
 *
 * @author zhongxiang.wang
 */
@RestController
@RequestMapping("/api/v1/console/branchSession")
public class BranchSessionController {

    @Resource(type = BranchSessionService.class)
    private BranchSessionService branchSessionService;

    /**
     * Delete branch transaction
     *
     * @param xid      the branch of xid
     * @param branchId the branch  id
     * @return SingleResult<Void>
     */
    @DeleteMapping("deleteBranchSession")
    public SingleResult<Void> deleteBranchSession(String xid, String branchId) {
        return branchSessionService.deleteBranchSession(xid, branchId);
    }

    /**
     * Stop branch transaction retry
     *
     * @param xid      the branch of xid
     * @param branchId the branch  id
     * @return SingleResult<Void>
     */
    @PutMapping("stopBranchSession")
    public SingleResult<Void> stopBranchSession(String xid, String branchId) {
        return branchSessionService.stopBranchRetry(xid, branchId);
    }

    /**
     * Start branch transaction retry
     *
     * @param xid      the branch of xid
     * @param branchId the branch  id
     * @return SingleResult<Void>
     */
    @PutMapping("startBranchSession")
    public SingleResult<Void> startBranchRetry(String xid, String branchId) {
        return branchSessionService.startBranchRetry(xid, branchId);
    }
}
