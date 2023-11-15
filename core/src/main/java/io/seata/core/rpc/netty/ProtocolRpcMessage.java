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
package io.seata.core.rpc.netty;

import io.seata.core.protocol.AbstractIdentifyRequest;
import io.seata.core.protocol.RpcMessage;

/**
 * The protocol RPC message.
 *
 * @author Bughue
 */
public interface ProtocolRpcMessage {
    RpcMessage protocolMsg2RpcMsg();

    void rpcMsg2ProtocolMsg(RpcMessage rpcMessage);

    static String getVersion(Object body) {
        if(body instanceof AbstractIdentifyRequest){
            return  ((AbstractIdentifyRequest) body).getVersion();
        }else {
            //todo?
            return null;
        }
    }

    static void setVersion(Object body, String version){
        if(body instanceof AbstractIdentifyRequest){
            ((AbstractIdentifyRequest) body).setVersion(version);
        }else {
            //todo?
        }
    }
}
