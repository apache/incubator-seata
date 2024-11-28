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
package io.seata.core.rpc.netty;

import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.tm.DefaultTransactionManager;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TmClient Test
 **/
public class TmClientTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TmClientTest.class);
    private static TransactionManager tm = null;

    public static void testTm() throws Exception {
        TransactionManager tm = getTm();

        //globalBegin:TYPE_GLOBAL_BEGIN = 1 , TYPE_GLOBAL_BEGIN_RESULT = 2
        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID,
                ProtocolTestConstants.SERVICE_GROUP, "test", 60000);
        LOGGER.info("globalBegin ok:xid=" + xid);

        //globalCommit:TYPE_GLOBAL_COMMIT = 7 , TYPE_GLOBAL_COMMIT_RESULT = 8
        GlobalStatus commit = tm.commit(xid);
        LOGGER.info("globalCommit ok:" + commit);
        Assertions.assertEquals(commit, GlobalStatus.Committed);

        //globalRollback:TYPE_GLOBAL_ROLLBACK = 9 , TYPE_GLOBAL_ROLLBACK_RESULT = 10
        GlobalStatus rollback = tm.rollback(xid);
        LOGGER.info("globalRollback ok:" + rollback);
        Assertions.assertEquals(rollback, GlobalStatus.Rollbacked);

        //getStatus:TYPE_GLOBAL_STATUS = 15 , TYPE_GLOBAL_STATUS_RESULT = 16
        GlobalStatus status = tm.getStatus(xid);
        LOGGER.info("getStatus ok:" + status);
        Assertions.assertEquals(status, GlobalStatus.Rollbacked);

        //globalReport:TYPE_GLOBAL_REPORT = 17 , TYPE_GLOBAL_REPORT_RESULT = 18 (0.6.1 not support)
//        GlobalStatus globalReport = tm.globalReport(xid, GlobalStatus.Committed);

        GlobalStatus rollback2 = tm.rollback(xid);
        LOGGER.info("globalRollback ok:" + rollback2);
    }

    public static TransactionManager getTm() {
        if (tm == null) {
            synchronized (TmClientTest.class) {
                if (tm == null) {
                    TmRpcClient tmRpcClient = TmRpcClient.getInstance(ProtocolTestConstants.APPLICATION_ID,
                            ProtocolTestConstants.SERVICE_GROUP);
                    tmRpcClient.init();
                    tm = new DefaultTransactionManager();
                }
            }
        }
        return tm;
    }


}
