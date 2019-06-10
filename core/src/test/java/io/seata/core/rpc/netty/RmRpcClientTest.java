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