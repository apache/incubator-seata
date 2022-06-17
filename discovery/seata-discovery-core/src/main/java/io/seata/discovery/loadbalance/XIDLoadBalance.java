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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;

/**
 * The type xid load balance.
 *
 * @author funkye
 */
@LoadLevel(name = XID_LOAD_BALANCE)
public class XIDLoadBalance implements LoadBalance {
    
    private static final LoadBalance RANDOM_LOAD_BALANCE = EnhancedServiceLoader.load(LoadBalance.class,
        LoadBalanceFactory.RANDOM_LOAD_BALANCE);

    private static final String SPLIT = ":";

    @Override
    public <T> T select(List<T> invokers, String xid) throws Exception {
        if (StringUtils.isNotBlank(xid) && xid.contains(SPLIT)) {
            String[] xidArray = xid.split(SPLIT);
            int port = Integer.parseInt(xidArray[1]);
            String ip = xidArray[0];
            for (T invoker : invokers) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress)invoker;
                if (StringUtils.equals(ip, inetSocketAddress.getHostName()) && inetSocketAddress.getPort() == port) {
                    return (T)inetSocketAddress;
                }
            }
            throw new RuntimeException("not found seata-server channel");
        }
        return RANDOM_LOAD_BALANCE.select(invokers, xid);
    }

}
