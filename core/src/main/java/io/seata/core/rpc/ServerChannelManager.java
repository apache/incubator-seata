package io.seata.core.rpc;

import java.util.Map;

import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;

/**
 * @author goodboycoder
 */
public interface ServerChannelManager {
    /**
     * Register tm channel.
     *
     * @param request the request
     * @param channel the channel
     * @throws IncompatibleVersionException the incompatible version exception
     */
    void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException;

    /**
     * Register rm channel.
     *
     * @param request the resource manager request
     * @param channel                the channel
     * @throws IncompatibleVersionException the incompatible  version exception
     */
    void registerRMChannel(RegisterRMRequest request, SeataChannel channel) throws IncompatibleVersionException;

    /**
     * Gets get channel.
     *
     * @param resourceId Resource ID
     * @param clientId   Client ID - ApplicationId:IP:Port
     * @return Corresponding channel, NULL if not found.
     */
    SeataChannel getChannel(String resourceId, String clientId);

    /**
     * get rm channels
     *
     * @return Map<String, SeataChannel>
     */
    Map<String, SeataChannel> getRmChannels();

    /**
     * Is registered boolean.
     *
     * @param channel the channel
     * @return the boolean
     */
    boolean isRegistered(SeataChannel channel);

    /**
     * Gets get context from identified.
     *
     * @param channel the channel
     * @return the get context from identified
     */
    RpcContext getContextFromIdentified(SeataChannel channel);

    /**
     * Release rpc context.
     *
     * @param channel the channel
     */
    void releaseRpcContext(SeataChannel channel);
}
