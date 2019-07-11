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

import io.netty.buffer.ByteBuf;
import io.seata.common.Constants;
import io.seata.common.util.StringUtils;

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

    public static HeadMapSerializer getInstance() {
        return INSTANCE;
    }

    /**
     * 简单 map 的序列化过程, 用来序列化 bolt 的 header
     *
     * @param map bolt header
     * @return 序列化后的 byte 数组
     */
    public int encode(Map<String, String> map, ByteBuf out) {
        if (map == null || map.isEmpty() || out == null) {
            return 0;
        }
        int start = out.writerIndex();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                writeString(out, key);
                writeString(out, value);
            }
        }
        return out.writerIndex() - start;
    }

    /**
     * 简单 map 的反序列化过程, 用来反序列化 bolt 的 header
     * <p>
     *
     * @param in bolt header
     * @return 反序列化后的 Map 对象
     */
    public Map<String, String> decode(ByteBuf in, int length) {
        Map<String, String> map = new HashMap<String, String>();
        if (in == null || in.readableBytes() == 0 || length == 0) {
            return map;
        }
        int tick = in.readerIndex();
        while (in.readerIndex() - tick < length) {
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
     */
    protected void writeString(ByteBuf out, String str) {
        if (str == null) {
            out.writeInt(-1);
        } else if (str.isEmpty()) {
            out.writeInt(0);
        } else {
            byte[] bs = str.getBytes(Constants.DEFAULT_CHARSET);
            out.writeInt(bs.length);
            out.writeBytes(bs);
        }
    }

    /**
     * 读取一个字符串
     *
     * @param in 输入流程
     * @return 字符串
     */
    protected String readString(ByteBuf in) {
        int length = in.readInt();
        if (length < 0) {
            return null;
        } else if (length == 0) {
            return StringUtils.EMPTY;
        } else {
            byte[] value = new byte[length];
            in.readBytes(value);
            return new String(value, Constants.DEFAULT_CHARSET);
        }
    }
}
