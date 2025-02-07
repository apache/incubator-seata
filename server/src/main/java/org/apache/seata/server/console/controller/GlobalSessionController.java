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
import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global Session Controller
 */
@RestController
@RequestMapping("/api/v1/console/globalSession")
public class GlobalSessionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionController.class);

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

    /**
     * Delete the global session
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @DeleteMapping("deleteGlobalSession")
    public SingleResult<Void> deleteGlobalSession(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global session, xid: {}", xid);
        }
        return globalSessionService.deleteGlobalSession(xid);
    }

    /**
     * Delete the global session
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @DeleteMapping("forceDeleteGlobalSession")
    public SingleResult<Void> forceDeleteGlobalSession(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to force delete the global session, xid: {}", xid);
        }
        return globalSessionService.forceDeleteGlobalSession(xid);
    }

    /**
     * Stop the global session retry
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @PutMapping("stopGlobalSession")
    public SingleResult<Void> stopGlobalSession(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the global session, xid: {}", xid);
        }
        return globalSessionService.stopGlobalRetry(xid);
    }

    /**
     * Start the global session retry
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @PutMapping("startGlobalSession")
    public SingleResult<Void> startGlobalSession(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the global session, xid: {}", xid);
        }
        return globalSessionService.startGlobalRetry(xid);
    }

    /**
     * Send global session to commit or rollback to rm
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @PutMapping("sendCommitOrRollback")
    public SingleResult<Void> sendCommitOrRollback(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to commit or rollback the global session, xid: {}", xid);
        }
        return globalSessionService.sendCommitOrRollback(xid);
    }

    /**
     * Change the global session status
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @PutMapping("changeGlobalStatus")
    public SingleResult<Void> changeGlobalStatus(String xid) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to change the global session, xid: {}", xid);
        }
        return globalSessionService.changeGlobalStatus(xid);
    }
}
