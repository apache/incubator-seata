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

import io.netty.channel.Channel;
import io.seata.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.SocketAddress;

/**
 * @author ph3636
 */
public class ChannelUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelUtil.class);

    /**
     * get address from channel
     * @param channel the channel
     * @return address
     */
    public static String getAddressFromChannel(Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();
        String address = socketAddress.toString();
        if (socketAddress.toString().indexOf(Constants.ENDPOINT_BEGIN_CHAR) == 0) {
            address = socketAddress.toString().substring(Constants.ENDPOINT_BEGIN_CHAR.length());
        }
        return address;
    }

    /**
     * get client ip from channel
     * @param channel the channel
     * @return client ip
     */
    public static String getClientIpFromChannel(Channel channel) {
        String address = getAddressFromChannel(channel);
        String clientIp = address;
        if (clientIp.contains(Constants.IP_PORT_SPLIT_CHAR)) {
            clientIp = clientIp.substring(0, clientIp.lastIndexOf(Constants.IP_PORT_SPLIT_CHAR));
        }
        return clientIp;
    }

    /**
     * get client port from channel
     * @param channel the channel
     * @return client port
     */
    public static Integer getClientPortFromChannel(Channel channel) {
        String address = getAddressFromChannel(channel);
        Integer port = 0;
        try {
            if (address.contains(Constants.IP_PORT_SPLIT_CHAR)) {
                port = Integer.parseInt(address.substring(address.lastIndexOf(Constants.IP_PORT_SPLIT_CHAR) + 1));
            }
        } catch (NumberFormatException exx) {
            LOGGER.error(exx.getMessage());
        }
        return port;
    }
}
