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
package io.seata.apm.skywalking.plugin.common;

import io.netty.channel.Channel;
import io.seata.core.protocol.RpcMessage;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

/**
 * @author zhaoyuguang
 */
public class SWSeataUtils {

    public static String convertPeer(Channel channel) {
        String peer = channel.remoteAddress().toString();
        if (peer.startsWith("/")) {
            peer = peer.substring(1);
        }
        return peer;
    }

    public static String convertOperationName(RpcMessage rpcMessage) {
        String requestSimpleName = rpcMessage.getBody().getClass().getSimpleName();
        if (SeataPluginConfig.Plugin.SEATA.SERVER) {
            return ComponentsDefine.SEATA.getName() + "/TC/" + requestSimpleName;
        }
        if (SWSeataConstants.isTransactionManagerOperationName(requestSimpleName)) {
            return ComponentsDefine.SEATA.getName() + "/TM/" + requestSimpleName;
        }
        return ComponentsDefine.SEATA.getName() + "/RM/" + requestSimpleName;
    }
}
