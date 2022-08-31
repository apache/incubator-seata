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
package io.seata.core.rpc.grpc.filter;

import java.net.InetSocketAddress;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.ServerTransportFilter;
import io.seata.common.util.StringUtils;
import io.seata.core.rpc.grpc.ContextKeyConstants;
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
        String connectionId = transportAttrs.get(ContextKeyConstants.CONNECT_ID);
        if (StringUtils.isNotBlank(connectionId)) {
            //TODO unregister connection

        }
        super.transportTerminated(transportAttrs);
    }
}
