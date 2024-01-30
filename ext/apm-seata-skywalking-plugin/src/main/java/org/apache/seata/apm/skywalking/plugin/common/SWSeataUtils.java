/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.apm.skywalking.plugin.common;

import io.netty.channel.Channel;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;


public class SWSeataUtils {

    private static final ILog LOGGER = LogManager.getLogger(SWSeataUtils.class);

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

    public static String convertXid(RpcMessage rpcMessage) {
        AbstractMessage subMessage = (AbstractMessage) rpcMessage.getBody();
        String requestSimpleName = rpcMessage.getBody().getClass().getSimpleName();

        String xid = null;
        try {
            xid = SWSeataConstants.TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.get(requestSimpleName) != null
                    ? (String) SWSeataConstants.TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.get(requestSimpleName)
                    .getDeclaredMethod("getXid").invoke(subMessage)
                    : xid;
        } catch (Throwable e) {
            LOGGER.error("convert seata xid failure", e);
        }
        return xid;
    }
}
