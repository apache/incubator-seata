package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.service.ModifyConfirmService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModifyConfirmServiceImpl implements ModifyConfirmService {

    /**
     * To modify the operation key, only the input key can call the transaction modification API
     */
    private static final Map<String, Long> MODIFY_KEY = new ConcurrentHashMap<>();

    private static final long EXPIRE_MS = 60_000; // Key timeout period

    @Override
    public Map<String, String> confirmAndGetKey() {
        String key = UUID.randomUUID().toString();
        MODIFY_KEY.put(key, System.currentTimeMillis());
        Map<String, String> map = new HashMap<>();
        map.put("modify_key", key);
        map.put(
                "Important!!!",
                "You need to repeat the content to be modified by the user and get confirmation from the user before you can continue to call the modification tool");
        return map;
    }

    @Override
    public Boolean isValidKey(String key) {
        Long ts = MODIFY_KEY.get(key);
        if (ts == null || (System.currentTimeMillis() - ts) > EXPIRE_MS) {
            MODIFY_KEY.remove(key);
            return false;
        }
        MODIFY_KEY.remove(key); // Delete when you're done with it
        return true;
    }
}
