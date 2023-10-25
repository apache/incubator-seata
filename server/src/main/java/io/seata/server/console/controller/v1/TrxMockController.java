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

package io.seata.server.console.controller.v1;

import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.core.exception.TransactionException;
import io.seata.server.Server;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static io.seata.server.console.Constant.DEMO_APP_NAME;
import static io.seata.server.console.Constant.DEMO_GROUP_NAME;
import static io.seata.server.console.Constant.DEMO_TXN_NAME;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/console/trx")
public class TrxMockController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrxMockController.class);

    @RequestMapping(method = RequestMethod.POST, value = "begin")
    public SingleResult<String> tryBegin(@RequestParam("timeout") int timeout) {
        DefaultCore core = new DefaultCore(Server.getNettyRemotingServer());
        if (timeout < 3000) timeout = 3000;
        try {
            String xid = core.begin(DEMO_APP_NAME, DEMO_GROUP_NAME, DEMO_TXN_NAME, timeout);
            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return SingleResult.failure(Code.ERROR);
            } else {
                return SingleResult.success(xid);
            }
        } catch (TransactionException e) {
            LOGGER.error(e.getMessage());
            return SingleResult.failure(Code.ERROR.code, e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "commit")
    public SingleResult<String> tryCommit(@RequestParam("xid") String xid) {
        DefaultCore core = new DefaultCore(Server.getNettyRemotingServer());
        try {
            core.commit(xid);
            return SingleResult.success(xid);
        } catch (TransactionException e) {
            LOGGER.error(e.getMessage());
            return SingleResult.failure(Code.ERROR.code, e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "rollback")
    public SingleResult<String> tryRollback(@RequestParam("xid") String xid) {
        DefaultCore core = new DefaultCore(Server.getNettyRemotingServer());
        try {
            core.rollback(xid);
            return SingleResult.success(xid);
        } catch (TransactionException e) {
            LOGGER.error(e.getMessage());
            return SingleResult.failure(Code.ERROR.code, e.getMessage());
        }
    }
}
