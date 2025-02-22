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

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.service.BranchSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Branch Session Controller
 */
@RestController
@RequestMapping("/api/v1/console/branchSession")
public class BranchSessionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSessionController.class);

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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        return branchSessionService.deleteBranchSession(xid, branchId);
    }

    /**
     * Delete branch transaction
     *
     * @param xid      the branch of xid
     * @param branchId the branch  id
     * @return SingleResult<Void>
     */
    @DeleteMapping("forceDeleteBranchSession")
    public SingleResult<Void> forceDeleteBranchSession(String xid, String branchId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        return branchSessionService.forceDeleteBranchSession(xid, branchId);
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the branch session, xid: {} branchId: {}", xid, branchId);
        }
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the branch session, xid: {} branchId: {}", xid, branchId);
        }
        return branchSessionService.startBranchRetry(xid, branchId);
    }
}
