package com.alibaba.fescar.tm.api.transaction;

import com.alibaba.fastjson.JSON;
import io.seata.tm.api.transaction.NoRollbackRule;
import io.seata.tm.api.transaction.RollbackRule;
import io.seata.tm.api.transaction.TransactionInfo;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author guoyao
 * @date 2019/4/17
 */
public class TransactionInfoTest {

    private static final String EXCEPTION_NAME = "java.lang.Exception";
    private static final String IO_EXCEPTION_NAME = "IOException";
    private static final String NAME = "test";
    private static final int TIME_OUT = 30000;


    /**
     * Test field get set from json.
     */
    @Test
    public void testFieldGetSetFromJson() {
        String fromJson = "{\n" +
                "\t\"name\":\""+NAME+"\",\n" +
                "\t\"timeOut\":"+TIME_OUT+"\n" +
                "}";
        TransactionInfo fromTxInfo = JSON.parseObject(fromJson, TransactionInfo.class);
        assertThat(fromTxInfo.getTimeOut()).isEqualTo(TIME_OUT);
        assertThat(fromTxInfo.getName()).isEqualTo(NAME);

        TransactionInfo toTxInfo = new TransactionInfo();
        toTxInfo.setTimeOut(TIME_OUT);
        toTxInfo.setName(NAME);
        String toJson = JSON.toJSONString(toTxInfo, true);
        assertThat(fromJson).isEqualTo(toJson);
    }


    @Test
    public void testRollBackOn() {
        TransactionInfo txInfo = new TransactionInfo();
        //default true
        assertTrue(txInfo.rollBackOn(new IllegalArgumentException()));
        assertTrue(txInfo.rollBackOn(new Exception()));
        assertTrue(txInfo.rollBackOn(new IOException()));
        assertTrue(txInfo.rollBackOn(new NullPointerException()));

        Set<RollbackRule> sets = new LinkedHashSet<>();
        sets.add(new RollbackRule(EXCEPTION_NAME));
        sets.add(new RollbackRule(IllegalArgumentException.class));
        sets.add(new NoRollbackRule(IO_EXCEPTION_NAME));
        sets.add(new NoRollbackRule(NullPointerException.class));


        txInfo.setRollbackRules(sets);

        assertTrue(txInfo.rollBackOn(new IllegalArgumentException()));
        assertTrue(txInfo.rollBackOn(new Exception()));
        assertFalse(txInfo.rollBackOn(new IOException()));
        assertFalse(txInfo.rollBackOn(new NullPointerException()));

        // not found return true
        assertTrue(txInfo.rollBackOn(new RuntimeException()));
    }
}
