package com.alibaba.fescar.tm.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;
import com.alibaba.fescar.metrics.Measurement;
import com.alibaba.fescar.tm.DefaultTransactionManager;
import com.alibaba.fescar.tm.metrics.RegistryManager;

public class DefaultGlobalTransactionForMetricsTest {
  @Test
  public void test() throws TransactionException {
    String xid = UUID.randomUUID().toString();

    DefaultTransactionManager.set(new TransactionManager() {
      @Override
      public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) {
        return xid;
      }

      @Override
      public GlobalStatus commit(String xid) {
        return null;
      }

      @Override
      public GlobalStatus rollback(String xid) {
        return null;
      }

      @Override
      public GlobalStatus getStatus(String xid) {
        return null;
      }
    });

    DefaultGlobalTransaction transaction = new DefaultGlobalTransaction();
    transaction.begin(10000, "test_transaction");

    Map<String, Measurement> measurements = new HashMap<>();
    RegistryManager.get().getRegistry().measure()
        .forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));

    Assert.assertEquals(1, measurements.size());
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,name=test_transaction,role=tm,status=active)").getValue(), 0);

    //tm commit transaction
    transaction.commit();

    measurements.clear();
    RegistryManager.get().getRegistry().measure()
        .forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(9, measurements.size());
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=counter,name=test_transaction,role=tm,status=active)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,name=test_transaction,role=tm,status=committed)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=count,status=committed)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=total,status=committed)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=timer,name=test_transaction,role=tm,statistic=count,status=committed)").getValue(), 0);

    //let us start a new transaction and rollback it
    transaction = new DefaultGlobalTransaction();
    transaction.begin(10000, "test_transaction");
    transaction.rollback();

    measurements.clear();
    RegistryManager.get().getRegistry().measure()
        .forEach(measurement -> measurements.put(measurement.getId().toString(), measurement));
    Assert.assertEquals(17, measurements.size());
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,name=test_transaction,role=tm,status=committed)").getValue(), 0);
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=count,status=committed)").getValue(), 0);
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=total,status=committed)").getValue(), 0);
    Assert.assertEquals(0, measurements.get("fescar.transaction(meter=timer,name=test_transaction,role=tm,statistic=count,status=committed)").getValue(), 0);

    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=counter,name=test_transaction,role=tm,status=rollback)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=count,status=rollback)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=summary,name=test_transaction,role=tm,statistic=total,status=rollback)").getValue(), 0);
    Assert.assertEquals(1, measurements.get("fescar.transaction(meter=timer,name=test_transaction,role=tm,statistic=count,status=rollback)").getValue(), 0);
  }
}
