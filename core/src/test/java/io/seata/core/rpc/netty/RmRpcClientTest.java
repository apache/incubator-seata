package io.seata.core.rpc.netty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rm RPC client test.
 *
 * @author zhaojun
 */
class RmRpcClientTest {
    
    @Test
    public void assertGetInstanceAfterDestroy() {
        RmRpcClient oldClient = RmRpcClient.getInstance("ap", "group");
        AtomicBoolean initialized = getInitializeStatus(oldClient);
        oldClient.init();
        assertTrue(initialized.get());
        oldClient.destroy();
        assertFalse(initialized.get());
        RmRpcClient newClient = RmRpcClient.getInstance("ap", "group");
        Assertions.assertNotEquals(oldClient, newClient);
        initialized = getInitializeStatus(newClient);
        assertFalse(initialized.get());
        newClient.init();
        assertTrue(initialized.get());
        newClient.destroy();
    }
    
    private AtomicBoolean getInitializeStatus(final RmRpcClient rmRpcClient) {
        try {
            Field field = rmRpcClient.getClass().getDeclaredField("initialized");
            field.setAccessible(true);
            return (AtomicBoolean) field.get(rmRpcClient);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}