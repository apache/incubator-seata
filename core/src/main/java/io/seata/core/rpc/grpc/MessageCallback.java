package io.seata.core.rpc.grpc;

/**
 * @author goodboycoder
 */
public interface MessageCallback {
    void onSuccess(Object result);

    void onException(Throwable err);
}
