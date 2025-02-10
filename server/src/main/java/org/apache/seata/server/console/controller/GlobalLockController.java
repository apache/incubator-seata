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

import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.param.GlobalLockParam;
import org.apache.seata.server.console.vo.GlobalLockVO;
import org.apache.seata.server.console.service.GlobalLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Global Lock Controller
 */
@RestController
@RequestMapping("/api/v1/console/globalLock")
public class GlobalLockController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLockController.class);

    @Resource(type = GlobalLockService.class)
    private GlobalLockService globalLockService;

    /**
     * Query locks by param
     * @param param the param
     * @return the list of GlobalLockVO
     */
    @GetMapping("query")
    public PageResult<GlobalLockVO> query(@ModelAttribute GlobalLockParam param) {
        return globalLockService.query(param);
    }

    /**
     * Delete global locks
     *
     * @param  param the param
     * @return SingleResult<Void>
     */
    @DeleteMapping("delete")
    public SingleResult<Void> delete(@ModelAttribute GlobalLockParam param) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global lock, param: {}", param);
        }
        return globalLockService.deleteLock(param);
    }

    /**
     * Check if the lock exist the branch session
     *
     * @param xid      xid
     * @param branchId branch id
     * @return the list of GlobalLockVO
     */
    @GetMapping("check")
    public SingleResult<Boolean> check(String xid, String branchId) {
        return globalLockService.check(xid, branchId);
    }
}
