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

import com.alibaba.fastjson.JSON;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 * @date 2019/4/17
 */
public class TransactionInfoTest {

    private static final String IO_EXCEPTION_SHORT_NAME = "IOException";
    private static final String NAME = "test";
    private static final int TIME_OUT = 30000;


    /**
     * Test field get set from json.
     */
    @Test
    public void testFieldGetSetFromJson() {
        String fromJson = "{\n" +
                "\t\"name\":\""+ NAME +"\",\n" +
                "\t\"rollbackRules\":[{\n" +
                "\t\t\"exceptionName\":\""+ Exception.class.getName() +"\"\n" +
                "\t},{\n" +
                "\t\t\"exceptionName\":\""+ IllegalArgumentException.class.getName() +"\"\n" +
                "\t},{\n" +
                "\t\t\"exceptionName\":\""+ IO_EXCEPTION_SHORT_NAME +"\"\n" +
                "\t},{\n" +
                "\t\t\"exceptionName\":\""+ NullPointerException.class.getName() +"\"\n" +
                "\t}],\n" +
                "\t\"timeOut\":30000\n" +
                "}";
        TransactionInfo fromTxInfo = JSON.parseObject(fromJson, TransactionInfo.class);
        assertThat(fromTxInfo.getTimeOut()).isEqualTo(TIME_OUT);
        assertThat(fromTxInfo.getName()).isEqualTo(NAME);
        assertThat(fromTxInfo.getRollbackRules()).isEqualTo(getRollbackRules());

        TransactionInfo toTxInfo = new TransactionInfo();
        toTxInfo.setTimeOut(TIME_OUT);
        toTxInfo.setName(NAME);
        toTxInfo.setRollbackRules(getRollbackRules());
        String toJson = JSON.toJSONString(toTxInfo, true);
        assertThat(fromJson).isEqualTo(toJson);
    }


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
        assertThat(txInfo.rollbackOn(new Exception())).isTrue();
        assertThat(txInfo.rollbackOn(new IOException())).isFalse();
        assertThat(txInfo.rollbackOn(new NullPointerException())).isFalse();

        // not found return true
        assertThat(txInfo.rollbackOn(new RuntimeException())).isTrue();
    }

    private Set<RollbackRule> getRollbackRules() {
        Set<RollbackRule> sets = new LinkedHashSet<>();
        sets.add(new RollbackRule(Exception.class.getName()));
        sets.add(new RollbackRule(IllegalArgumentException.class));
        sets.add(new NoRollbackRule(IO_EXCEPTION_SHORT_NAME));
        sets.add(new NoRollbackRule(NullPointerException.class));
        return sets;
    }
}
