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
package io.seata.discovery.loadbalance;

import static io.seata.discovery.loadbalance.LoadBalanceFactory.XID_LOAD_BALANCE;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type xid load balance.
 *
 * @author funkye
 */
@LoadLevel(name = XID_LOAD_BALANCE)
public class XIDLoadBalance implements LoadBalance {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(XIDLoadBalance.class);

    private static final LoadBalance RANDOM_LOAD_BALANCE = EnhancedServiceLoader.load(LoadBalance.class,
        LoadBalanceFactory.RANDOM_LOAD_BALANCE);

    private static final String SPLIT = ":";

    @Override
    public <T> T select(List<T> invokers, String xid) throws Exception {
        if (StringUtils.isNotBlank(xid) && xid.contains(SPLIT)) {
            // ip:port:transactionId -> ip:port
            String serverAddress = xid.substring(0, xid.lastIndexOf(SPLIT));
            // ip:port -> port
            int index = serverAddress.lastIndexOf(SPLIT);
            int port = Integer.parseInt(serverAddress.substring(index + 1));
            // ipv4/v6
            String ip = serverAddress.substring(0, index);
            InetSocketAddress xidInetSocketAddress = new InetSocketAddress(ip, port);
            for (T invoker : invokers) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress)invoker;
                if (Objects.equals(xidInetSocketAddress, inetSocketAddress)) {
                    return (T)inetSocketAddress;
                }
            }
            LOGGER.error("not found seata-server channel,xid: {}, try use random load balance", xid);
        }
        return RANDOM_LOAD_BALANCE.select(invokers, xid);
    }

}
