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
package io.seata.tm.api.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 */
public class TransactionHookManagerTest {

    @AfterEach
    public void clear() {
        TransactionHookManager.clear();
    }

    @Test
    public void testRegisterHook() {
        TransactionHookAdapter transactionHookAdapter = new TransactionHookAdapter();
        TransactionHookManager.registerHook(transactionHookAdapter);
        List<TransactionHook> hooks = TransactionHookManager.getHooks();
        assertThat(hooks).isNotEmpty();
        assertThat(hooks.get(0)).isEqualTo(transactionHookAdapter);
    }

    @Test
    public void testGetHooks() {
        assertThat(TransactionHookManager.getHooks()).isEmpty();
        TransactionHookManager.registerHook(new TransactionHookAdapter());
        assertThat(TransactionHookManager.getHooks()).isNotEmpty();
    }

    @Test
    public void testClear() {
        assertThat(TransactionHookManager.getHooks()).isEmpty();
        TransactionHookManager.registerHook(new TransactionHookAdapter());
        assertThat(TransactionHookManager.getHooks()).isNotEmpty();
        TransactionHookManager.clear();
        assertThat(TransactionHookManager.getHooks()).isEmpty();
    }
    @Test
    public void testNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> TransactionHookManager.registerHook(null));
    }
}
