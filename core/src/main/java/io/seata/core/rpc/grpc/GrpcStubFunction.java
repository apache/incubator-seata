package io.seata.core.rpc.grpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.seata.core.rpc.SeataChannel;

/**
 * @author goodboycoder
 */
@FunctionalInterface
public interface GrpcStubFunction<T extends Message, S extends Message> {

    ListenableFuture<S> apply(SeataChannel channel, T req);
}
