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
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.console.result.PageResult;
import io.seata.server.console.vo.GlobalSessionVO;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global Session Controller
 * @author zhongxiang.wang
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

    /**
     * Delete the global session
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @DeleteMapping("deleteGlobalSession")
    public SingleResult<Void> deleteGlobalSession(String xid) {
        return globalSessionService.deleteGlobalSession(xid);
    }

    /**
     * Stop the global session retry
     *
     * @param xid The xid
     * @return SingleResult<Void>
     */
    @PutMapping("stopGlobalSession")
    public SingleResult<Void> stopGlobalSession(String xid) {
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
        return globalSessionService.changeGlobalStatus(xid);
    }


}
