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

import org.apache.seata.common.XID;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.config.ConfigurationFactory;

import static org.apache.seata.common.ConfigurationKeys.SERVER_REGISTRY_PORT_CAMEL;

/**
 * Resolves the transaction endpoint that this server instance advertises to registry centers and clients.
 *
 * <p>The advertised port can differ from the RPC listen port, for example when the server runs behind a Docker port
 * mapping ({@code -p 21908:8091}) or a firewall that only opens the mapped port. Every path that builds the transaction
 * endpoint of the current server instance - the instance registration, the Raft cluster metadata and the vGroup mapping
 * notification - must resolve the port through this class, otherwise clients can be redirected to the unreachable
 * listen port.
 *
 * <p>The advertised port is read from {@code seata.server.registry-port} ({@code server.registryPort} or the
 * {@code SEATA_REGISTRY_PORT} environment variable) and falls back to the RPC listen port when it is not configured.
 */
public final class AdvertisedEndpointResolver {

    private AdvertisedEndpointResolver() {}

    /**
     * Resolve the advertised transaction port of the current server instance.
     *
     * @return the configured registry port if it is present and valid, otherwise the RPC listen port
     */
    public static int resolveTransactionPort() {
        int registryPort =
                NumberUtils.toInt(ConfigurationFactory.getInstance().getConfig(SERVER_REGISTRY_PORT_CAMEL), 0);
        return registryPort > 0 ? registryPort : XID.getPort();
    }
}
