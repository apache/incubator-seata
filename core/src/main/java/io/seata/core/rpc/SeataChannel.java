package io.seata.core.rpc;

import java.net.SocketAddress;

/**
 * @author goodboycoder
 */
public interface SeataChannel {

    /**
     * Unique ID that identifies the channel
     * @return channel ID
     */
    String getId();

    /**
     * Rpc communication type of channel
     * @return rpcType
     */
    RpcType getType();

    /**
     * Original channel
     * @return channel
     */
    Object originChannel();

    /**
     * Returns the remote address where this channel is connected to
     * @return socketAddress
     */
    SocketAddress remoteAddress();

    /**
     * Return true if the Channel is active and so connected.
     * @return true if the channel is active
     */
    boolean isActive();

    /**
     * Request to close the Channel
     */
    void close();

    /**
     * Request to disconnect from the remote peer
     */
    void disconnect();

    /**
     * send message to remote peer with no ack
     * @param msg message to be sent
     */
    void sendMsg(Object msg);
}
