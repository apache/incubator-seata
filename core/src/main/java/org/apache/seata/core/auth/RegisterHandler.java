package org.apache.seata.core.auth;

import io.netty.channel.Channel;
import org.apache.seata.core.protocol.RegisterRMResponse;

public interface RegisterHandler {
    /**
     * On a register response received.
     *
     * @param response received response message
     * @param channel  channel of the response
     */
    void onRegisterResponse(RegisterRMResponse response, Channel channel, Integer rpcId);
}
