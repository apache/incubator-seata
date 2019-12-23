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

import io.seata.core.protocol.RpcMessage;

import java.util.concurrent.TimeoutException;

/**
 * The interface Client message sender.
 *
 * @author slievrly
 */
public interface ClientMessageSender {
    /**
     * Send msg with response object.
     *
     * @param msg     the msg
     * @param timeout the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(Object msg, long timeout) throws TimeoutException;

    /**
     * Send msg with response object.
     *
     * @param serverAddress the server address
     * @param msg           the msg
     * @param timeout       the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(String serverAddress, Object msg, long timeout) throws TimeoutException;

    /**
     * Send msg with response object.
     *
     * @param msg the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    Object sendMsgWithResponse(Object msg) throws TimeoutException;

    /**
     * Send response.
     *
     * @param request       the msg id
     * @param serverAddress the server address
     * @param msg           the msg
     */
    void sendResponse(RpcMessage request, String serverAddress, Object msg);
}
