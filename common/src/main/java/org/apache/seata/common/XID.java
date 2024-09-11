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
package org.apache.seata.common;

import static org.apache.seata.common.Constants.IP_PORT_SPLIT_CHAR;


/**
 * The type Xid.
 *
 */
public class XID {

    private static int port;

    private static String ipAddress;

    /**
     * Sets port.
     *
     * @param port the port
     */
    public static void setPort(int port) {
        XID.port = port;
    }

    /**
     * Sets ip address.
     *
     * @param ipAddress the ip address
     */
    public static void setIpAddress(String ipAddress) {
        XID.ipAddress = ipAddress;
    }

    /**
     * Generate xid string.
     *
     * @param tranId the tran id
     * @return the string
     */
    public static String generateXID(long tranId) {
        return new StringBuilder().append(ipAddress).append(IP_PORT_SPLIT_CHAR).append(port).append(IP_PORT_SPLIT_CHAR).append(tranId).toString();
    }

    /**
     * Gets transaction id.
     *
     * @param xid the xid
     * @return the transaction id
     */
    public static long getTransactionId(String xid) {
        if (xid == null) {
            return -1;
        }

        int idx = xid.lastIndexOf(":");
        return Long.parseLong(xid.substring(idx + 1));
    }

    /**
     * Gets port.
     *
     * @return the port
     */
    public static int getPort() {
        return port;
    }

    /**
     * Gets ip address.
     *
     * @return the ip address
     */
    public static String getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets ipAddress:port
     *
     * @return eg: 127.0.0.1:8091
     */
    public static String getIpAddressAndPort() {
        return new StringBuilder().append(ipAddress).append(IP_PORT_SPLIT_CHAR).append(port).toString();
    }
}
