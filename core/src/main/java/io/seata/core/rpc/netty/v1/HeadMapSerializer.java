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
package io.seata.core.rpc.netty.v1;

import io.seata.common.Constants;
import io.seata.common.io.UnsafeByteArrayInputStream;
import io.seata.common.io.UnsafeByteArrayOutputStream;
import io.seata.common.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Common serializer of map (this generally refers to header).
 *
 * @author Geng Zhang
 * @since 0.7.0
 */
public class HeadMapSerializer {
    
    private static final HeadMapSerializer INSTANCE = new HeadMapSerializer();

    private HeadMapSerializer() {
        
    }
    
    public static HeadMapSerializer getInstance(){
        return INSTANCE;
    }
    
    /**
     * 简单 map 的序列化过程, 用来序列化 bolt 的 header
     *
     * @param map bolt header
     * @return 序列化后的 byte 数组
     */
    public byte[] encode(Map<String, String> map) throws IOException {
        if (map == null || map.isEmpty()) {
            return null;
        }
        UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream(64);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                writeString(out, key);
                writeString(out, value);
            }
        }
        return out.toByteArray();
    }

    /**
     * 简单 map 的反序列化过程, 用来反序列化 bolt 的 header
     * <p>
     *
     * @param bytes bolt header
     * @return 反序列化后的 Map 对象
     */
    public Map<String, String> decode(byte[] bytes) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        if (bytes == null || bytes.length == 0) {
            return map;
        }

        UnsafeByteArrayInputStream in = new UnsafeByteArrayInputStream(bytes);
        while (in.available() > 0) {
            String key = readString(in);
            String value = readString(in);
            map.put(key, value);
        }

        return map;
    }

    /**
     * 写一个String
     *
     * @param out 输出流
     * @param str 字符串
     * @throws IOException 写入异常
     */
    protected void writeString(OutputStream out, String str) throws IOException {
        if (str == null) {
            writeInt(out, -1);
        } else if (str.isEmpty()) {
            writeInt(out, 0);
        } else {
            byte[] bs = str.getBytes(Constants.DEFAULT_CHARSET);
            writeInt(out, bs.length);
            out.write(bs);
        }
    }

    /**
     * 读取一个字符串
     *
     * @param in 输入流程
     * @return 字符串
     * @throws IOException 读取异常
     */
    protected String readString(InputStream in) throws IOException {
        int length = readInt(in);
        if (length < 0) {
            return null;
        } else if (length == 0) {
            return StringUtils.EMPTY;
        } else {
            byte[] value = new byte[length];
            in.read(value);
            return new String(value, Constants.DEFAULT_CHARSET);
        }
    }

    /**
     * OutputStream.write(int) 仅 write 第一个 byte, 而不是整个 int
     *
     * @param out OutputStream
     * @param i   int value
     * @throws IOException if an I/O error occurs.
     */
    private void writeInt(OutputStream out, int i) throws IOException {
        out.write((byte) (i >> 24));
        out.write((byte) (i >> 16));
        out.write((byte) (i >> 8));
        out.write(i);
    }

    /**
     * InputStream.read 仅 read 一个 byte
     *
     * @param in InputStream
     * @return int value
     * @throws IOException if an I/O error occurs.
     */
    public int readInt(InputStream in) throws IOException {
        return ((byte) in.read() & 0xff) << 24
                | ((byte) in.read() & 0xff) << 16
                | ((byte) in.read() & 0xff) << 8
                | (byte) in.read() & 0xff;
    }
}
