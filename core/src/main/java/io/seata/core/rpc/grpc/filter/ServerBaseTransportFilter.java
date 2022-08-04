package io.seata.core.rpc.grpc.filter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.ServerTransportFilter;
import io.seata.core.rpc.grpc.ContextKeyConstants;
import io.seata.core.rpc.grpc.GrpcServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.Constants.IP_PORT_SPLIT_CHAR;

/**
 * @author goodboycoder
 */
public class ServerBaseTransportFilter extends ServerTransportFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerBaseTransportFilter.class);

    @Override
    public Attributes transportReady(Attributes transportAttrs) {
        InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (null == remoteAddress) {
            throw new IllegalStateException("can not get remoteAddress from transportAttrs");
        }

        //Generate connection ID based on remote address and current time
        String connectionId = remoteAddress.getAddress().getHostAddress() + IP_PORT_SPLIT_CHAR +
                remoteAddress.getPort() + "_" + System.currentTimeMillis();
        LOGGER.info("Transport ready for connection:" + connectionId);
        return transportAttrs.toBuilder().set(ContextKeyConstants.CONNECT_ID, connectionId).build();
    }

    @Override
    public void transportTerminated(Attributes transportAttrs) {
        super.transportTerminated(transportAttrs);
    }
}
