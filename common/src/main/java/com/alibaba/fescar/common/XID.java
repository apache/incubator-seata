/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/10/10 12:27
 * @FileName: XID
 * @Description:
 */
public class XID {

    private static int port;

    private static String ipAddress;

    public static void setPort(int port) {
        XID.port = port;
    }

    public static void setIpAddress(String ipAddress) {
        XID.ipAddress = ipAddress;
    }

    public static String generateXID(long tranId) {
        return ipAddress + ":" + port + ":" + tranId;
    }

    public static long getTransactionId(String xid) {
        if (xid == null) {
            return -1;
        }

        int idx = xid.lastIndexOf(":");
        return Long.parseLong(xid.substring(idx + 1));
    }

    public static String getServerAddress(String xid) {
        if (xid == null) {
            return null;
        }

        int idx = xid.lastIndexOf(":");
        return xid.substring(0, idx);
    }
}
