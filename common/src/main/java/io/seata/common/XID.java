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
package io.seata.common;

/**
 * The type Xid.
 *
 * @author slievrly
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
        return ipAddress + ":" + port + ":" + tranId;
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
}
