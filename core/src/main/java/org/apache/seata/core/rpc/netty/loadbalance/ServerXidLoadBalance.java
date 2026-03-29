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
package org.apache.seata.core.rpc.netty.loadbalance;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.rpc.netty.ChannelUtil;
import org.apache.seata.core.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * XID load balance for server side.
 */
@LoadLevel(name = "XID")
public class ServerXidLoadBalance implements ServerLoadBalance {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerXidLoadBalance.class);
    private static final String SPLIT = ":";
    private static final ServerLoadBalance RANDOM_LOAD_BALANCE =
            EnhancedServiceLoader.load(ServerLoadBalance.class, "RandomLoadBalance");

    @Override
    public RpcContext select(List<RpcContext> candidates, String xid) {
        if (StringUtils.isNotBlank(xid)) {
            String targetAddress = extractAddressFromXid(xid);
            if (StringUtils.isNotBlank(targetAddress)) {
                for (RpcContext candidate : candidates) {
                    if (candidate == null || candidate.getChannel() == null) {
                        continue;
                    }
                    String candidateAddress = ChannelUtil.getAddressFromChannel(candidate.getChannel());
                    if (Objects.equals(targetAddress, candidateAddress)) {
                        return candidate;
                    }
                }
            }
            LOGGER.warn("not found target rm channel by xid: {}, fallback to random", xid);
        }
        return RANDOM_LOAD_BALANCE.select(candidates, xid);
    }

    private String extractAddressFromXid(String xid) {
        int lastSplitIndex = xid.lastIndexOf(SPLIT);
        if (lastSplitIndex <= 0) {
            return null;
        }
        return xid.substring(0, lastSplitIndex);
    }
}
