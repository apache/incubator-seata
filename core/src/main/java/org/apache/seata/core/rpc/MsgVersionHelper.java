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
package org.apache.seata.core.rpc;

import io.netty.channel.Channel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.MessageTypeAware;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;

import java.util.Arrays;
import java.util.List;

/**
 * the type ServerSkipMsgHelper
 **/
public class MsgVersionHelper {

    private static final List<Short> SKIP_MSG_CODE_V0 = Arrays.asList(MessageType.TYPE_RM_DELETE_UNDOLOG);

    public static boolean versionNotSupport(Channel channel, RpcMessage rpcMessage) {
        if (rpcMessage == null || rpcMessage.getBody() == null || channel == null) {
            return false;
        }
        Object msg = rpcMessage.getBody();
        String version = Version.getChannelVersion(channel);
        if (StringUtils.isBlank(version) || msg == null) {
            return false;
        }
        boolean isV0 = Version.isV0(version);
        if (!isV0 || !(msg instanceof MessageTypeAware)) {
            return false;
        }
        short typeCode = ((MessageTypeAware) msg).getTypeCode();
        return SKIP_MSG_CODE_V0.contains(typeCode);
    }
}
