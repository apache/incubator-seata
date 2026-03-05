package com.alibaba.fescar.server;

import org.testng.annotations.Test;

public class UUIDGeneratorOverflowTest {
    private static final int UUID_GENERATE_COUNT = 5;
    private static final int SERVER_NODE_ID = 2;

    @Test
    public void testGenerateUUID() {
        UUIDGenerator.init(SERVER_NODE_ID);
        for (int i = 0; i < UUID_GENERATE_COUNT; i++) {
            System.out.println("[UUID " + i + "] is: "+ UUIDGenerator.generateUUID());
        }
    }
}