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
package io.seata.core.rpc.grpc.norelativeimpl;

import io.seata.core.protocol.RpcMessage;
import java.util.concurrent.TimeoutException;
import io.netty.channel.Channel;


public interface RpcCallableServer {


    Object sendSyncRequest(String resourceId, String clientId, Object msg) throws TimeoutException;


    Object sendSyncRequest(Channel conn, Object msg) throws TimeoutException;


    void sendAsyncRequest(Channel conn, Object msg);


    void sendAsyncResponse(RpcMessage rpcMessage, Channel conn, Object msg);



}
