package io.seata.core.rpc;

import java.net.SocketAddress;

import io.seata.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class SeataChannelUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataChannelUtil.class);

    /**
     * get address from channel
     * @param channel the channel
     * @return address
     */
    public static String getAddressFromChannel(SeataChannel channel) {
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
    public static String getClientIpFromChannel(SeataChannel channel) {
        String clientIp = getAddressFromChannel(channel);
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
    public static Integer getClientPortFromChannel(SeataChannel channel) {
        String address = getAddressFromChannel(channel);
        int port = 0;
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
