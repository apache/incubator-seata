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

import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.core.exception.TransactionException;
import io.seata.server.Server;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/admin/txn")
public class TxnTryController {

    private Logger logger = LoggerFactory.getLogger(TxnTryController.class);

    private final String DEMO_APP_NAME = "demo-app";

    private final String DEMO_GROUP_NAME = "demo-group";

    private final String DEMO_TXN_NAME = "txn-name";

    @RequestMapping
    public SingleResult<String> tryASimpleTxn() {
        DefaultCore core = new DefaultCore(Server.getNettyRemotingServer());
        try {
            String xid = core.begin(DEMO_APP_NAME, DEMO_GROUP_NAME, DEMO_TXN_NAME, 3000);
            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return SingleResult.failure(Code.ERROR);
            } else {
                return SingleResult.success(xid);
            }
        } catch (TransactionException e) {
            logger.error(e.getMessage());
            return SingleResult.failure(Code.ERROR);
        }
    }
}
