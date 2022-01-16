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
package io.seata.server.auth;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.NetUtil;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;

/**
 * @author slievrly
 */
@LoadLevel(name = "defaultCheckAuthHandler", order = 100)
public class DefaultCheckAuthHandler extends AbstractCheckAuthHandler {

    BlackList blackList = new BlackList(ConfigurationKeys.BLACKLIST);

    @Override
    public boolean doRegTransactionManagerCheck(RegisterTMRequest request, ChannelHandlerContext ctx) {
        return doIpCheck(ctx);
    }

    @Override
    public boolean doRegResourceManagerCheck(RegisterRMRequest request, ChannelHandlerContext ctx) {
        return doIpCheck(ctx);
    }

    private boolean doIpCheck(ChannelHandlerContext ctx) {
        String ip = NetUtil.toStringAddress(ctx.channel().remoteAddress()).split(":")[0];
        if (blackList.contains(ip)) {
            return false;
        }
        return true;
    }
}
