package org.apache.seata.integration.tx.api.interceptor.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IfNeedEnhanceBeanTest {

    private IfNeedEnhanceBean enhanceBean;


    @BeforeEach
    public void setUp() {
        enhanceBean = new IfNeedEnhanceBean();
    }

    @Test
    public void testIsEnhanceNeeded() {
        enhanceBean.setIfNeed(true);
        assertTrue(enhanceBean.isIfNeed());
    }

    @Test
    public void testGetNeedEnhanceEnum() {
        enhanceBean.setNeedEnhanceEnum(NeedEnhanceEnum.SERVICE_BEAN);
        assertEquals(NeedEnhanceEnum.SERVICE_BEAN, enhanceBean.getNeedEnhanceEnum());
    }
}
