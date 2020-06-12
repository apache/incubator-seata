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
package io.seata.rm.transaction;

import io.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wang.liang
 */
public class RMTransactionHookManagerTest {

    @Test
    public void testGetHooks() {
        assertThat(RMTransactionHookManager.getHooks()).isEmpty();
        RMTransactionHookManager.registerGlobalHook(new RMTransactionHookAdapter());
        assertThat(RMTransactionHookManager.getHooks()).isNotEmpty();
    }

    @Test
    public void testRegisterGlobalHook() {
        RMTransactionHookAdapter RMTransactionHookAdapter = new RMTransactionHookAdapter();
        RMTransactionHookManager.registerGlobalHook(RMTransactionHookAdapter);
        List<RMTransactionHook> hooks = RMTransactionHookManager.getHooks();
        assertThat(hooks).isNotEmpty();
        assertThat(hooks.get(0)).isEqualTo(RMTransactionHookAdapter);
    }

    @Test
    public void testTriggerHooks() {
        RMTransactionHookManager.registerGlobalHook(new RMTransactionHook() {
            @Override
            public void beforeBranchCommit(BranchType branchType, String xid, long branchId) {
                throw new RuntimeException();
            }
        });

        //no throw
        RMTransactionHookManager.triggerHooks(null, 0, (hook) -> {
            hook.beforeBranchCommit(null, null, 0);
        });
    }

    @Test
    public void testNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            RMTransactionHookManager.registerGlobalHook(null);
        });
    }
}
