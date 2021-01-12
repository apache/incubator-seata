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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 */
public class TransactionInfoTest {

    private static final String IO_EXCEPTION_SHORT_NAME = "IOException";

    @Test
    public void testRollBackOn() {
        TransactionInfo txInfo = new TransactionInfo();

        //default true
        assertThat(txInfo.rollbackOn(new IllegalArgumentException())).isTrue();
        assertThat(txInfo.rollbackOn(new Exception())).isTrue();
        assertThat(txInfo.rollbackOn(new IOException())).isTrue();
        assertThat(txInfo.rollbackOn(new NullPointerException())).isTrue();

        Set<RollbackRule> sets = getRollbackRules();
        txInfo.setRollbackRules(sets);

        assertThat(txInfo.rollbackOn(new IllegalArgumentException())).isTrue();
        assertThat(txInfo.rollbackOn(new IllegalStateException())).isTrue();
        assertThat(txInfo.rollbackOn(new IOException())).isFalse();
        assertThat(txInfo.rollbackOn(new NullPointerException())).isFalse();

        // not found return false
        assertThat(txInfo.rollbackOn(new MyRuntimeException("test"))).isFalse();
        assertThat(txInfo.rollbackOn(new RuntimeException())).isFalse();
        assertThat(txInfo.rollbackOn(new Throwable())).isFalse();
    }

    private Set<RollbackRule> getRollbackRules() {
        Set<RollbackRule> sets = new LinkedHashSet<>();
        sets.add(new RollbackRule(IllegalStateException.class.getName()));
        sets.add(new RollbackRule(IllegalArgumentException.class));
        sets.add(new NoRollbackRule(IO_EXCEPTION_SHORT_NAME));
        sets.add(new NoRollbackRule(NullPointerException.class));
        return sets;
    }
}
