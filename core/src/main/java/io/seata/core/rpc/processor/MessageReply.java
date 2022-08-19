package io.seata.core.rpc.processor;

/**
 * @author goodboycoder
 */
public interface MessageReply {
    void reply(final Object response);
}
