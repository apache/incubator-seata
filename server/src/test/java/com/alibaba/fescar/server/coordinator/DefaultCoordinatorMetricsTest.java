/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.server.coordinator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.common.loader.EnhancedServiceLoader;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackResponse;
import com.alibaba.fescar.core.rpc.RpcContext;
import com.alibaba.fescar.metrics.Measurement;
import com.alibaba.fescar.metrics.Registry;
import com.alibaba.fescar.server.session.SessionHolder;

public class DefaultCoordinatorMetricsTest {
  @Test
  public void test() throws TransactionException, IOException {
    DefaultCoordinator coordinator = new DefaultCoordinator(null);
    Registry registry = EnhancedServiceLoader.load(Registry.class);
    SessionHolder.init(null);

    //start a transaction
    GlobalBeginRequest request = new GlobalBeginRequest();
    request.setTransactionName("test_transaction");
    GlobalBeginResponse response = new GlobalBeginResponse();
    coordinator.doGlobalBegin(request, response, new RpcContext());

    Map<String, Measurement> measurements = new HashMap<>();
    registry.measure().forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));

    Assert.assertEquals(1, measurements.size());
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,role=tc,status=active)").getValue(), 0);

    //commit this transaction
    GlobalCommitRequest commitRequest = new GlobalCommitRequest();
    commitRequest.setTransactionId(XID.getTransactionId(response.getXid()));
    coordinator.doGlobalCommit(commitRequest, new GlobalCommitResponse(), new RpcContext());

    measurements.clear();
    registry.measure().forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(9, measurements.size());
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=counter,role=tc,status=active)").getValue(), 0);
    Assert
        .assertEquals(1, measurements.get("fescar.transaction(meter=counter,role=tc,status=committed)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=count,status=committed)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=total,status=committed)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=timer,role=tc,statistic=count,status=committed)").getValue(), 0);

    //start another new transaction
    request = new GlobalBeginRequest();
    request.setTransactionName("test_transaction_2");
    response = new GlobalBeginResponse();
    coordinator.doGlobalBegin(request, response, new RpcContext());

    //rollback this transaction
    GlobalRollbackRequest rollbackRequest = new GlobalRollbackRequest();
    rollbackRequest.setTransactionId(XID.getTransactionId(response.getXid()));
    coordinator.doGlobalRollback(rollbackRequest, new GlobalRollbackResponse(), new RpcContext());

    measurements.clear();
    registry.measure().forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(17, measurements.size());
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=counter,role=tc,status=active)").getValue(), 0);

    Assert
        .assertEquals(1, measurements.get("fescar.transaction(meter=counter,role=tc,status=committed)").getValue(), 0);
    Assert.assertEquals(0,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=count,status=committed)").getValue(), 0);
    Assert.assertEquals(0,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=total,status=committed)").getValue(), 0);
    Assert.assertEquals(0,
        measurements.get("fescar.transaction(meter=timer,role=tc,statistic=count,status=committed)").getValue(), 0);

    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,role=tc,status=rollback)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=count,status=rollback)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=summary,role=tc,statistic=total,status=rollback)").getValue(), 0);
    Assert.assertEquals(1,
        measurements.get("fescar.transaction(meter=timer,role=tc,statistic=count,status=rollback)").getValue(), 0);
  }
}
