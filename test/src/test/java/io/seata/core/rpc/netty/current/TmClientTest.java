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
package io.seata.core.rpc.netty.current;


import io.netty.channel.Channel;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.tm.DefaultTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.rpc.netty.ChannelManagerTestHelper.getChannel;

/**
 * TmClient Test
 *
 **/
public class TmClientTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TmClientTest.class);
    public static void main(String[] args) throws Exception {

        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(
                ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP);
        tmNettyRemotingClient.init();
        TransactionManager tm = new DefaultTransactionManager();

        //register:TYPE_REG_CLT = 101 , TYPE_REG_CLT_RESULT = 102
        TmNettyRemotingClient client = TmNettyRemotingClient.getInstance();
        Channel channel = getChannel(client);
        LOGGER.info("TM register ok:channel=" + channel);

        //globalBegin:TYPE_GLOBAL_BEGIN = 1 , TYPE_GLOBAL_BEGIN_RESULT = 2
        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID,
                ProtocolTestConstants.SERVICE_GROUP, "test", 60000);
        LOGGER.info("globalBegin ok:xid=" + xid);

//        if (xid == null) {
//            xid = "6";
//        }

        //globalCommit:TYPE_GLOBAL_COMMIT = 7 , TYPE_GLOBAL_COMMIT_RESULT = 8
        GlobalStatus commit = tm.commit(xid);
        LOGGER.info("globalCommit ok:" + commit);

        //globalRollback:TYPE_GLOBAL_ROLLBACK = 9 , TYPE_GLOBAL_ROLLBACK_RESULT = 10
        GlobalStatus rollback = tm.rollback(xid);
        LOGGER.info("globalRollback ok:" + rollback);


        //getStatus:TYPE_GLOBAL_STATUS = 15 , TYPE_GLOBAL_STATUS_RESULT = 16
        GlobalStatus status = tm.getStatus(xid);
        LOGGER.info("getStatus ok:" + status);

        //globalReport:TYPE_GLOBAL_REPORT = 17 , TYPE_GLOBAL_REPORT_RESULT = 18
        GlobalStatus globalReport = tm.globalReport(xid, GlobalStatus.Committed);
        LOGGER.info("globalReport ok:" + globalReport);

    }



}
