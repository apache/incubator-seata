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
package io.seata.server.coordinator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.exception.TransactionException;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.metrics.Measurement;
import io.seata.server.metrics.MetricsManager;
import io.seata.server.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static io.seata.server.coordinator.DefaultCoordinatorTest.MockServerMessageSender;

/**
 * Test Metrics
 *
 * @author zhengyangyong
 */
@SpringBootTest
@Disabled
public class DefaultCoordinatorMetricsTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) throws InterruptedException {
        EnhancedServiceLoader.unloadAll();
        //wait for the pre test asynCommit operation finished.
        Thread.sleep(2000);
        MetricsManager.get().getRegistry().clearUp();
    }

    @Test
    public void test() throws IOException, TransactionException, InterruptedException {
        DefaultCoordinator coordinator = DefaultCoordinator.getInstance(null);
        coordinator.setRemotingServer(new MockServerMessageSender());
        SessionHolder.init(null);
        try {
            //start a transaction
            GlobalBeginRequest request = new GlobalBeginRequest();
            request.setTransactionName("test_transaction");
            GlobalBeginResponse response = new GlobalBeginResponse();
            coordinator.doGlobalBegin(request, response, new RpcContext());
            Thread.sleep(2000);
            Map<String, Measurement> measurements = new HashMap<>();
            MetricsManager.get().getRegistry().measure().forEach(
                measurement -> measurements.put(measurement.getId().toString(), measurement));

            Assertions.assertEquals(1, measurements.size());
            Assertions.assertEquals(1,
                measurements.get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=active)")
                    .getValue(), 0);

            //commit this transaction
            GlobalCommitRequest commitRequest = new GlobalCommitRequest();
            commitRequest.setXid(response.getXid());
            coordinator.doGlobalCommit(commitRequest, new GlobalCommitResponse(), new RpcContext());

            measurements.clear();
            //we need sleep for a short while because default canBeCommittedAsync() is true
            Thread.sleep(2000);

            MetricsManager.get().getRegistry().measure().forEach(
                measurement -> measurements.put(measurement.getId().toString(), measurement));
            Assertions.assertEquals(9, measurements.size());
            Assertions.assertEquals(0,
                measurements.get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=active)")
                    .getValue(), 0);
            Assertions.assertEquals(1, measurements
                .get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=count,"
                    + "status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=total,"
                    + "status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=timer,role=tc,statistic=count,status=committed)")
                .getValue(), 0);

            //start another new transaction
            request = new GlobalBeginRequest();
            request.setTransactionName("test_transaction_2");
            response = new GlobalBeginResponse();
            coordinator.doGlobalBegin(request, response, new RpcContext());

            //rollback this transaction
            GlobalRollbackRequest rollbackRequest = new GlobalRollbackRequest();
            rollbackRequest.setXid(response.getXid());
            coordinator.doGlobalRollback(rollbackRequest, new GlobalRollbackResponse(), new RpcContext());

            measurements.clear();
            Thread.sleep(2000);
            MetricsManager.get().getRegistry().measure().forEach(
                measurement -> measurements.put(measurement.getId().toString(), measurement));
            Assertions.assertEquals(17, measurements.size());
            Assertions.assertEquals(0,
                measurements.get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=active)")
                    .getValue(), 0);

            Assertions.assertEquals(1, measurements
                .get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(0, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=count,"
                    + "status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(0, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=total,"
                    + "status=committed)")
                .getValue(), 0);
            Assertions.assertEquals(0, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=timer,role=tc,statistic=count,status=committed)")
                .getValue(), 0);

            Assertions.assertEquals(1, measurements
                .get("seata.transaction(applicationId=null,group=null,meter=counter,role=tc,status=rollbacked)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=count,"
                    + "status=rollbacked)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=summary,role=tc,statistic=total,"
                    + "status=rollbacked)")
                .getValue(), 0);
            Assertions.assertEquals(1, measurements.get(
                "seata.transaction(applicationId=null,group=null,meter=timer,role=tc,statistic=count,"
                    + "status=rollbacked)")
                .getValue(), 0);
        } finally {
            // call SpringContextShutdownHook
        }
    }

}
