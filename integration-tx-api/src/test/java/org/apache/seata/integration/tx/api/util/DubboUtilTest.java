package org.apache.seata.integration.tx.api.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DubboUtilTest {

    @Test
    public void testIsDubbo3XPartialProxyName() {
        assertTrue(DubboUtil.isDubboProxyName(SimpleDubboProxy.class.getName()));
    }

    @Test
    public void testIsNotDubboProxyName() {
        assertFalse(DubboUtil.isDubboProxyName(ArrayList.class.getName()));
    }

    @Test
    public void testGetAssistInterfaceForNull() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        assertNull(DubboUtil.getAssistInterface(null));
    }

    @Test
    public void testGetAssistInterfaceForNotDubboProxy() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        assertNull(DubboUtil.getAssistInterface(new ArrayList<>()));
    }

    @Test
    public void testGetAssistInterfaceThrowsException() {
        assertThrows(NoSuchFieldException.class, () -> DubboUtil.getAssistInterface(new SimpleDubboProxy()));
    }
}
