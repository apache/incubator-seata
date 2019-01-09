/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.rpc;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import io.netty.channel.Channel;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/10/15 16:56
 * @FileName: ServerMessageSender
 * @Description:
 */
public interface ServerMessageSender {
    /**
     * Send request to client by the upper level.
     * Actually, just BranchCommitRequest/BranchRollbackRequest will be sent from server to RM client.
     * No case for sending a request to TM client. ResourceId is useless by case of sending a TM request.
     *
     * @param resourceId resourceId
     * @param clientIP IP of the target client
     * @param applicationId applicationId of the target client
     * @param request request to be sent
     */
    void sendRequest(String resourceId, String clientIP, String applicationId, Object request /* TODO: this should be AbstractMessage */);

    /**
     * @param msgId
     * @param dbKey
     * @param clientIp
     * @param clientAppName
     * @param msg
     */
    void sendResponse(long msgId, String dbKey, String clientIp, String clientAppName, Object msg);

    /**
     * @param msgId
     * @param channel
     * @param msg
     */
    void sendResponse(long msgId, Channel channel, Object msg);

    /**
     * 同步调用client
     *
     * @param dbKey
     * @param clientIp
     * @param clientAppName
     * @param msg
     * @return
     * @throws IOException
     */
    Object sendSynRequest(String dbKey, String clientIp, String clientAppName,
                          Object msg, long timeout) throws IOException, TimeoutException;

    /**
     * 同步调用client
     *
     * @param dbKey
     * @param clientIp
     * @param clientAppName
     * @param msg
     * @return
     * @throws IOException
     */
    Object sendSynRequest(String dbKey, String clientIp, String clientAppName,
                          Object msg) throws IOException, TimeoutException;
}
