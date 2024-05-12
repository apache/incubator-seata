package org.apache.seata.integration.tx.api.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.Constants;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoPhaseBusinessActionParamTest {

    private TwoPhaseBusinessActionParam actionParam;

    @BeforeEach
    public void setUp() {
        actionParam = new TwoPhaseBusinessActionParam();
    }

    @Test
    public void testGetActionName() {
        actionParam.setActionName("business_action");
        assertEquals("business_action", actionParam.getActionName());
    }

    @Test
    public void testIsReportDelayed() {
        actionParam.setDelayReport(true);
        assertTrue(actionParam.getDelayReport());
    }

    @Test
    public void testIsCommonFenceUsed() {
        actionParam.setUseCommonFence(true);
        assertTrue(actionParam.getUseCommonFence());
    }

    @Test
    public void testFillBusinessActionContext() {
        Map<String, Object> businessActionContextMap = new HashMap<>(2);
        businessActionContextMap.put(Constants.COMMIT_METHOD, "commit");
        businessActionContextMap.put(Constants.USE_COMMON_FENCE, false);

        actionParam.setBusinessActionContext(businessActionContextMap);

        assertEquals("commit", actionParam.getBusinessActionContext().get(Constants.COMMIT_METHOD));
        assertFalse((Boolean) actionParam.getBusinessActionContext().get(Constants.USE_COMMON_FENCE));
    }

    @Test
    public void testGetBranchType() {
        actionParam.setBranchType(BranchType.TCC);
        assertEquals(BranchType.TCC, actionParam.getBranchType());
    }
}
