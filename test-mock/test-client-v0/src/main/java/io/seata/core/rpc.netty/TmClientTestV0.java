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
package io.seata.core.rpc.netty;


import io.netty.channel.Channel;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.tm.DefaultTransactionManager;
import io.seata.tm.TransactionManagerHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TmClient Test
 *
 * @author minghua.xie
 * @date 2023/11/10
 **/
public class TmClientTestV0 {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TmClientTestV0.class);


    public static void main(String[] args) throws Exception {
        LOGGER.info("[VO] TM begin");

        TmRpcClient tmRpcClient = TmRpcClient.getInstance(MockConstants.APPLICATION_ID, MockConstants.SERVICE_GROUP);
        tmRpcClient.init();
        TransactionManager tm = TransactionManagerHolder.get();

        //register:TYPE_REG_CLT = 101 , TYPE_REG_CLT_RESULT = 102
        Channel channel = tmRpcClient.connect(MockConstants.SERVER_ADDRESS);
        LOGGER.info("[VO] TM register ok:channel=" + channel);
        TmRpcClient.getInstance().sendRequest(channel, HeartbeatMessage.PING);
        LOGGER.info("[VO] TM heartbeat ok ");

        //globalBegin:TYPE_GLOBAL_BEGIN = 1 , TYPE_GLOBAL_BEGIN_RESULT = 2
        String xid = tm.begin(MockConstants.APPLICATION_ID, MockConstants.SERVICE_GROUP, "test", 60000);
        LOGGER.info("[VO] globalBegin ok:xid=" + xid);

        if (xid == null) {
            xid = "6";
        }

        //globalCommit:TYPE_GLOBAL_COMMIT = 7 , TYPE_GLOBAL_COMMIT_RESULT = 8
        GlobalStatus commit = tm.commit(xid);
        LOGGER.info("[VO] globalCommit ok:" + commit);

        //globalRollback:TYPE_GLOBAL_ROLLBACK = 9 , TYPE_GLOBAL_ROLLBACK_RESULT = 10
        GlobalStatus rollback = tm.rollback(xid);
        LOGGER.info("[VO] globalRollback ok:" + rollback);


        //getStatus:TYPE_GLOBAL_STATUS = 15 , TYPE_GLOBAL_STATUS_RESULT = 16
        GlobalStatus status = tm.getStatus(xid);
        LOGGER.info("[VO] getStatus ok:" + status);

        //globalReport:TYPE_GLOBAL_REPORT = 17 , TYPE_GLOBAL_REPORT_RESULT = 18
//        GlobalStatus globalReport = tm.globalReport(xid, GlobalStatus.Committed);
//        LOGGER.info("globalReport ok:" + globalReport);

    }

}
