package io.seata.metrics;

import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.metrics.service.MetricsManager;
import io.seata.metrics.service.MetricsPublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.seata.metrics.TCMeterIdConstants.COUNTER_ACTIVE;

public class RegistryMeterKeyTest {
    @Test
    public void testGetIdMeterKey() {
        Id id1 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);
        String meterKey1 = id1.getMeterKey();
        Id id2 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TC);
        String meterKey2 = id2.getMeterKey();
        Id id3 = COUNTER_ACTIVE.withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_TM);
        String meterKey3 = id3.getMeterKey();

        Assertions.assertNotEquals(meterKey2, meterKey1);
        Assertions.assertEquals(meterKey3, meterKey1);
    }

    @Test
    public void testClientMetrics() {
        MetricsManager.setRole(MetricsManager.ROLE_VALUE_CLIENT);
        MetricsManager.get().init();
        MetricsPublisher.postBranchEvent("gtx", BranchType.XA, 0, 1, IdConstants.METRICS_EVENT_STATUS_VALUE_BRANCH_REGISTER_SUCCESS, BranchStatus.Registered.name());
        Map<String, Measurement> branchMeasurements = new HashMap<>();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            // don't care
        }

        MetricsManager.get().getRegistry().measure().forEach(
                measurement -> branchMeasurements.put(measurement.getId().toString(), measurement));
        Assertions.assertEquals(6, branchMeasurements.size());
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,branchType=XA,group=null,meter=timer,metricsEventStatus=branchRegisterSuccess,role=rm,statistic=average,status=Registered)")
                .getValue(), 0);
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,branchType=XA,group=null,meter=counter,metricsEventStatus=branchRegisterSuccess,role=rm,status=Registered)")
                .getValue(), 0);
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,branchType=XA,group=null,meter=timer,metricsEventStatus=branchRegisterSuccess,role=rm,statistic=total,status=Registered)")
                .getValue(), 0);
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,branchType=XA,group=null,meter=timer,metricsEventStatus=branchRegisterSuccess,role=rm,statistic=count,status=Registered)")
                .getValue(), 0);
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,group=null,meter=counter,metricsEventStatus=active,role=rm)")
                .getValue(), 0);
        Assertions.assertEquals(1, branchMeasurements.get("seata.transaction(applicationId=null,branchType=XA,group=null,meter=timer,metricsEventStatus=branchRegisterSuccess,role=rm,statistic=max,status=Registered)")
                .getValue(), 0);

        MetricsManager.get().getRegistry().clearUp();

        MetricsPublisher.postGlobalTransaction("gtx", 0, 1, IdConstants.METRICS_EVENT_STATUS_VALUE_GLOBAL_BEGIN_SUCCESS, GlobalStatus.Begin.name());
        Map<String, Measurement> globalMeasurements = new HashMap<>();

        try {
            Thread.sleep(500);
        } catch (Exception e) {
            // don't care
        }
        MetricsManager.get().getRegistry().measure().forEach(
                measurement -> globalMeasurements.put(measurement.getId().toString(), measurement));
        Assertions.assertEquals(6, globalMeasurements.size());
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=timer,metricsEventStatus=globalBeginSuccess,role=tm,statistic=count,status=Begin)")
                .getValue(), 0);
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=timer,metricsEventStatus=globalBeginSuccess,role=tm,statistic=max,status=Begin)")
                .getValue(), 0);
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=counter,metricsEventStatus=active,role=tm)")
                .getValue(), 0);
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=counter,metricsEventStatus=globalBeginSuccess,role=tm,status=Begin)")
                .getValue(), 0);
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=timer,metricsEventStatus=globalBeginSuccess,role=tm,statistic=total,status=Begin)")
                .getValue(), 0);
        Assertions.assertEquals(1, globalMeasurements.get("seata.transaction(applicationId=null,group=null,meter=timer,metricsEventStatus=globalBeginSuccess,role=tm,statistic=average,status=Begin)")
                .getValue(), 0);
    }

}
