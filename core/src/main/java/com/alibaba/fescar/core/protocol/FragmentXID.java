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
import io.netty.util.NetUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import sun.net.util.IPAddressUtil;

/**
 * FragmentXID is used for support golang distributed cluster server supporting fragmentation
 *
 * @author fagongzi(zhangxu19830126 @ gmail.com)
 */
public class FragmentXID {
    private static final byte FLAG_IPV4 = 0;
    private static final byte FLAG_IPV6 = 1;

    /**
     * Fragment Id has fixed length for ipv4 format, ip(4bytes) + port(4bytes) + transactionId(8bytes) +
     * fragmentId(8bytes) = 24 bytes
     */
    public static final short FIXED_IPV4_BYTES = 24;

    /**
     * Fragment Id has fixed length for ipv6 format, ip(16bytes) + port(4bytes) + transactionId(8bytes) +
     * fragmentId(8bytes) = 36 bytes
     */
    public static final short FIXED_IPV6_BYTES = 36;

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
        this.ipAndPort = NetUtil.toSocketAddressString(this.ip, port);
    }

    private FragmentXID(ByteBuf buf) {
        byte flag = buf.readByte();
        if (flag != FLAG_IPV4 || flag != FLAG_IPV6) {
            throw new RuntimeException("create FragmentXID failed, invalid ip version flag: " + flag);
        }

        StringBuffer ip = new StringBuffer();
        if (flag == FLAG_IPV4) {
            if (buf.readableBytes() < FIXED_IPV4_BYTES) {
                throw new RuntimeException("create FragmentXID with ipv4 format failed, expect " + FIXED_IPV4_BYTES + " bytes but " + buf.readableBytes());
            }

            for (int i = 0; i < 4; i++) {
                ip.append(buf.readByte() & 0xff);
                if (i < 3) {
                    ip.append('.');
                }
            }
        } else {
            if (buf.readableBytes() < FIXED_IPV6_BYTES) {
                throw new RuntimeException("create FragmentXID with ipv6 format failed, expect " + FIXED_IPV6_BYTES + " bytes but " + buf.readableBytes());
            }

            for (int i = 0; i < 8; i++) {
                ip.append(buf.readByte() & 0xff);
                ip.append(buf.readByte() & 0xff);
                if (i < 7) {
                    ip.append(':');
                }
            }
        }

        this.ip = ip.toString();
        this.port = buf.readInt();
        this.transactionId = buf.readLong();
        this.fragmentId = buf.readLong();
        this.ipAndPort = NetUtil.toSocketAddressString(this.ip, port);
    }

    private FragmentXID(ByteBuffer buf) {
        byte flag = buf.get();
        if (flag != FLAG_IPV4 && flag != FLAG_IPV6) {
            throw new RuntimeException("create FragmentXID failed, invalid ip version flag: " + flag);
        }

        StringBuffer ip = new StringBuffer();
        if (flag == FLAG_IPV4) {
            if (buf.remaining() < FIXED_IPV4_BYTES) {
                throw new RuntimeException("create FragmentXID with ipv4 format failed, expect " + FIXED_IPV4_BYTES + " bytes but " + buf.remaining());
            }

            for (int i = 0; i < 4; i++) {
                ip.append(buf.get() & 0xff);
                if (i < 3) {
                    ip.append('.');
                }
            }
        } else {
            if (buf.remaining() < FIXED_IPV6_BYTES) {
                throw new RuntimeException("create FragmentXID with ipv6 format failed, expect " + FIXED_IPV6_BYTES + " bytes but " + buf.remaining());
            }

            for (int i = 0; i < 8; i++) {
                ip.append(buf.get() & 0xff);
                ip.append(buf.get() & 0xff);
                if (i < 7) {
                    ip.append(':');
                }
            }
        }

        this.ip = ip.toString();
        this.port = buf.getInt();
        this.transactionId = buf.getLong();
        this.fragmentId = buf.getLong();
        this.ipAndPort = NetUtil.toSocketAddressString(this.ip, port);
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

    /**
     * get FragmentXID from byte array
     *
     * @param buf byte buf value
     * @return FragmentXID
     */
    public static FragmentXID from(ByteBuffer buf) {
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
        if (IPAddressUtil.isIPv4LiteralAddress(ip)) {
            buf.writeByte(FLAG_IPV4);
        } else {
            buf.writeByte(FLAG_IPV6);
        }
        try {
            for (byte e : InetAddress.getByName(ip).getAddress()) {
                buf.writeByte(e & 0xff);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
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
            getFullIP().equals(xid.getFullIP());
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

    private String getFullIP() {
        try {
            return NetUtil.bytesToIpAddress(InetAddress.getByName(ip).getAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException((e));
        }
    }
}
