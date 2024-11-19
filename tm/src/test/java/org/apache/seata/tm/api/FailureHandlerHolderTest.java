package org.apache.seata.tm.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FailureHandlerHolderTest {

    @Test
    void testDefaultFailureHandler() {
        Assertions.assertTrue(FailureHandlerHolder.getFailureHandler() instanceof DefaultFailureHandlerImpl);
    }

    @Test
    void testSetFailureHandlerWithCustomHandler() {
        MockFailureHandlerHolder mockFailureHandlerHolder = new MockFailureHandlerHolder();

        FailureHandlerHolder.setFailureHandler(mockFailureHandlerHolder);

        Assertions.assertEquals(mockFailureHandlerHolder, FailureHandlerHolder.getFailureHandler());
    }
}
