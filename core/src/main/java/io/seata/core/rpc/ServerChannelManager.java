/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
