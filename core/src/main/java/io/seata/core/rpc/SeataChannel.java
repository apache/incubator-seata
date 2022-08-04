package io.seata.core.rpc;

import java.net.SocketAddress;

/**
 * @author goodboycoder
 */
public interface SeataChannel {

    String getId();

    RpcType getType();

    Object originChannel();

    SocketAddress remoteAddress();


}
