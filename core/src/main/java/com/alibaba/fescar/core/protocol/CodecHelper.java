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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Codec Helper
 *
 * @author fagongzi(zhangxu19830126 @ gmail.com)
 */
public class CodecHelper {
    public static final Charset UTF8 = Charset.forName("utf-8");

    private CodecHelper() {

    }

    /**
     * write the string value to buf
     *
     * @param buf the byte buf
     * @param value the value
     */
    public static void write(ByteBuffer buf, String value) {
        if (null == buf) {
            return;
        }

        if (null != value) {
            byte[] bs = value.getBytes(UTF8);
            buf.putShort((short) bs.length);
            if (bs.length > 0) {
                buf.put(bs);
            }
        } else {
            buf.putShort((short) 0);
        }
    }

    /**
     * write the long value to buf
     *
     * @param buf the byte buf
     * @param value the value
     */
    public static void write(ByteBuffer buf, long value) {
        buf.putLong(value);
    }

    /**
     * write the {@link FragmentXID} to buf
     *
     * @param buf the byte buf
     * @param value the value
     */
    public static void write(ByteBuffer buf, FragmentXID value) {
        if (null == buf || null == value) {
            return;
        }

        buf.put(value.toBytes());
    }

    /**
     * read the string value from the buf, using UTF-8
     *
     * @param in buf
     * @return UTF-8 string,  null if size is 0
     */
    public static String readString(ByteBuf in) {
        if (null == in) {
            return null;
        }

        short size = in.readShort();
        if (size == 0) {
            return null;
        }

        byte[] value = new byte[size];
        in.readBytes(value);
        return new String(value, UTF8);
    }

    /**
     * read the string value from the buf, using UTF-8
     *
     * @param in buf
     * @return UTF-8 string,  null if size is 0
     */
    public static String readBigString(ByteBuf in) {
        if (null == in) {
            return null;
        }

        int size = in.readInt();
        if (size == 0) {
            return null;
        }

        byte[] value = new byte[size];
        in.readBytes(value);
        return new String(value, UTF8);
    }

    /**
     * read the string value from the buf, using UTF-8
     *
     * @param buf buf
     * @return UTF-8 string,  null if size is 0
     */
    public static String readString(ByteBuffer buf) {
        if (null == buf) {
            return null;
        }

        short size = buf.getShort();
        if (size == 0) {
            return null;
        }

        byte[] value = new byte[size];
        buf.get(value);
        return new String(value, UTF8);
    }

    /**
     * read the long value from the buf
     *
     * @param in the buf
     * @return the value
     */
    public static long readLong(ByteBuf in) {
        if (null == in) {
            return 0;
        }

        return in.readLong();
    }

    /**
     * read the long value from the buf
     *
     * @param buf the buf
     * @return the value
     */
    public static long readLong(ByteBuffer buf) {
        if (null == buf) {
            return 0;
        }

        return buf.getLong();
    }

    /**
     * read byte array value from buf
     *
     * @param in the buf
     * @param size number of byte
     * @return the value
     */
    public static byte[] readBytes(ByteBuf in, int size) {
        if (null == in || size <= 0 || in.readableBytes() < size) {
            return null;
        }

        byte[] data = new byte[size];
        in.readBytes(data);
        return data;
    }

    /**
     * read byte array value from buf
     *
     * @param buf the buf
     * @param size number of byte
     * @return the value
     */
    public static byte[] readBytes(ByteBuffer buf, int size) {
        if (null == buf || size <= 0 || buf.remaining() < size) {
            return null;
        }

        byte[] data = new byte[size];
        buf.get(data);
        return data;
    }

    /**
     * read {@link FragmentXID} value from buf
     *
     * @param buf the buf
     * @return the value
     */
    public static FragmentXID readFragmentXID(ByteBuffer buf) {
        if (null == buf) {
            return null;
        }

        return FragmentXID.from(buf);
    }

    /**
     * read {@link FragmentXID} value from buf
     *
     * @param in the buf
     * @return the value
     */
    public static FragmentXID readFragmentXID(ByteBuf in) {
        if (null == in) {
            return null;
        }

        return FragmentXID.from(in);
    }
}
