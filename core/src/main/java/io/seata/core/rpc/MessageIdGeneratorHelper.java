package io.seata.core.rpc;

import io.seata.common.thread.PositiveAtomicCounter;

/**
 * @author goodboycoder
 */
public class MessageIdGeneratorHelper {
    private static final PositiveAtomicCounter ID_GENERATOR = new PositiveAtomicCounter();

    public static PositiveAtomicCounter getIdGenerator() {
        return ID_GENERATOR;
    }
}
