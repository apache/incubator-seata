package org.apache.seata.mcp.service;

import org.apache.seata.mcp.service.impl.ModifyConfirmServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModifyConfirmServiceTest {

    @Test
    void testConfirmAndGetKeyGeneratesMap() {
        ModifyConfirmService service = new ModifyConfirmServiceImpl();
        Map<String, String> map = service.confirmAndGetKey();
        assertNotNull(map.get("modify_key"));
        assertEquals("60s", map.get("expire_time"));
        assertTrue(map.containsKey("Important!!!"));
    }

    @Test
    void testIsValidKeyTrueAndThenRemoved() throws Exception {
        ModifyConfirmServiceImpl service = new ModifyConfirmServiceImpl();
        Map<String, String> map = service.confirmAndGetKey();
        String key = map.get("modify_key");
        assertTrue(service.isValidKey(key));
        // second time should be false since removed
        assertFalse(service.isValidKey(key));
    }

    @Test
    void testIsValidKeyFalseWhenNotExists() {
        ModifyConfirmService service = new ModifyConfirmServiceImpl();
        assertFalse(service.isValidKey("not-exist"));
    }

    @Test
    void testIsValidKeyExpired() throws Exception {
        ModifyConfirmServiceImpl service = new ModifyConfirmServiceImpl();
        Map<String, String> map = service.confirmAndGetKey();
        String key = map.get("modify_key");

        Field f = ModifyConfirmServiceImpl.class.getDeclaredField("MODIFY_KEY");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Long> store = (Map<String, Long>) f.get(null);
        // set ts to very old to force expire
        store.put(key, 0L);

        assertFalse(service.isValidKey(key));
    }
}
