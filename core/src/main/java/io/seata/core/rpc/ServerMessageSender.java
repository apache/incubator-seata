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

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.netty.channel.Channel;

/**
 * The interface Server message sender.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/15
 */
public interface ServerMessageSender {

    /**
     * Send response.
     *
     * @param msgId   the msg id
     * @param channel the channel
     * @param msg     the msg
     */
    void sendResponse(long msgId, Channel channel, Object msg);

    /**
     * Sync call to RM with timeout.
     *
     * @param resourceId Resource ID
     * @param clientId   Client ID
     * @param message    Request message
     * @param timeout    timeout of the call
     * @return Response message
     * @throws IOException .
     * @throws TimeoutException the timeout exception
     */
    Object sendSyncRequest(String resourceId, String clientId, Object message, long timeout)
        throws IOException, TimeoutException;

    /**
     * Sync call to RM
     *
     * @param resourceId Resource ID
     * @param clientId   Client ID
     * @param message    Request message
     * @return Response message
     * @throws IOException .
     * @throws TimeoutException the timeout exception
     */
    Object sendSyncRequest(String resourceId, String clientId, Object message) throws IOException, TimeoutException;
}
