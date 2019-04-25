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

/**
 * The enum Transport protocol type.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/10
 */
public enum TransportProtocolType {
    /**
     * Tcp transport protocol type.
     */
    TCP("tcp"),

    /**
     * Udt transport protocol type.
     */
    UDT("udt"),
    /**
     * Unix domain socket transport protocol type.
     */
    UNIX_DOMAIN_SOCKET("unix-domain-socket");

    /**
     * The Name.
     */
    public final String name;

    TransportProtocolType(String name) {
        this.name = name;
    }
}
