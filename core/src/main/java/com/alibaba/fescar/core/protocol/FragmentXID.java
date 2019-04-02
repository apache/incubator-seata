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

package com.alibaba.fescar.core.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * FragmentXID is used for support golang distributed cluster server supporting fragmentation
 *
 * @author fagongzi(zhangxu19830126 @ gmail.com)
 */
public class FragmentXID {
    /**
     * Fragment Id has fixed length, ip(4bytes) + port(4bytes) + transactionId(8bytes) + fragmentId(8bytes) = 24 bytes
     */
    public static final short FIXED_BYTES = 24;

    private static int serverPort = 8080;

    private static String serverIPAddress = "127.0.0.1";

    public static int getServerPort() {
        return serverPort;
    }

    public static void setServerPort(int serverPort) {
        FragmentXID.serverPort = serverPort;
    }

    public static String getServerIPAddress() {
        return serverIPAddress;
    }

    public static void setServerIPAddress(String serverIPAddress) {
        FragmentXID.serverIPAddress = serverIPAddress;
    }

    /**
     * The remote server ip and port
     */
    private String ipAndPort;

    /**
     * The remote server ip
     */
    private String ip;

    /**
     * The remote server port
     */
    private int port;

    /**
     * The global transaction Id
     */
    private long transactionId;

    /**
     * The fragment id in server
     */
    private long fragmentId;

    /**
     * create a FragmentXID from spec ip, port and fragment id
     *
     * @param transactionId the global transaction id
     */
    private FragmentXID(long transactionId) {
        this(serverIPAddress, serverPort, transactionId, 0);
    }

    /**
     * create a FragmentXID from spec ip, port and fragment id
     *
     * @param ip the ip
     * @param port the port
     * @param transactionId the global transaction id
     * @param fragmentId the fragment id
     */
    public FragmentXID(String ip, int port, long transactionId, long fragmentId) {
        this.ip = ip;
        this.port = port;
        this.transactionId = transactionId;
        this.fragmentId = fragmentId;
        this.ipAndPort = ip + ":" + port;
    }

    private FragmentXID(ByteBuf buf) {
        if (buf.readableBytes() < FIXED_BYTES) {
            throw new RuntimeException("create FragmentXID failed, expect " + FIXED_BYTES + " bytes but " + buf.readableBytes());
        }

        StringBuffer ip = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            ip.append(buf.readByte() & 0xff);
            if (i < 3) {
                ip.append('.');
            }
        }

        this.ip = ip.toString();
        this.port = buf.readInt();
        this.transactionId = buf.readLong();
        this.fragmentId = buf.readLong();
        this.ipAndPort = ip + ":" + port;
    }

    /**
     * get FragmentXID from byte array
     *
     * @param gid the transaction id
     * @return FragmentXID
     */
    public static FragmentXID from(long gid) {
        return new FragmentXID(gid);
    }

    /**
     * get FragmentXID from byte array
     *
     * @param hexValue hex string
     * @return FragmentXID
     */
    public static FragmentXID from(String hexValue) {
        if (StringUtils.isEmpty(hexValue)) {
            return null;
        }
        return FragmentXID.from(ByteBufUtil.decodeHexDump(hexValue));
    }

    /**
     * get FragmentXID from byte array
     *
     * @param data bytes value
     * @return FragmentXID
     */
    public static FragmentXID from(byte[] data) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(data);
        FragmentXID value = from(buf);
        buf.release();
        return value;
    }

    /**
     * get FragmentXID from byte array, the method don't release the byte buf
     *
     * @param buf byte buf value
     * @return FragmentXID
     */
    public static FragmentXID from(ByteBuf buf) {
        return new FragmentXID(buf);
    }

    public String getIpAndPort() {
        return ipAndPort;
    }

    /**
     * Gets the ip
     *
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * Gets the port
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the transaction Id
     *
     * @return the transaction Id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the fragment Id
     *
     * @return the fragment Id
     */
    public long getFragmentId() {
        return fragmentId;
    }

    /**
     * encode encode to bytes
     *
     * @return bytes
     */
    public byte[] toBytes() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        Arrays.stream(ip.split("\\.")).forEach(e -> buf.writeByte(Integer.valueOf(e) & 0xff));
        buf.writeInt(port);
        buf.writeLong(transactionId);
        buf.writeLong(fragmentId);

        byte[] value = new byte[buf.readableBytes()];
        buf.readBytes(value);
        buf.release();
        return value;
    }

    /**
     * Returns the hex string like this:  ff00010
     *
     * @return
     */
    public String toHexString() {
        return ByteBufUtil.hexDump(toBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FragmentXID xid = (FragmentXID) o;
        return port == xid.port &&
            transactionId == xid.transactionId &&
            fragmentId == xid.fragmentId &&
            ip.equals(xid.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, transactionId, fragmentId);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("ip=");
        result.append(ip);
        result.append(",");
        result.append("port=");
        result.append(port);
        result.append(",");
        result.append("transactionId=");
        result.append(transactionId);
        result.append(",");
        result.append("fragmentId=");
        result.append(fragmentId);
        return result.toString();
    }
}
