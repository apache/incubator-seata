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
package io.seata.core.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.seata.common.Constants;
import io.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * The type Abstract message.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/14
 */
public abstract class AbstractMessage implements MessageTypeAware, Serializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessage.class);

    protected static final long serialVersionUID = -1441020418526899889L;

    /**
     * The constant UTF8.
     */
    protected static final Charset UTF8 = Constants.DEFAULT_CHARSET;
    /**
     * The Ctx.
     */
    protected ChannelHandlerContext ctx;

    /**
     * Bytes to int int.
     *
     * @param bytes  the bytes
     * @param offset the offset
     * @return the int
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        int ret = 0;
        for (int i = 0; i < 4 && i + offset < bytes.length; i++) {
            ret <<= 8;
            ret |= (int)bytes[i + offset] & 0xFF;
        }
        return ret;
    }

    /**
     * Int to bytes.
     *
     * @param i      the
     * @param bytes  the bytes
     * @param offset the offset
     */
    public static void intToBytes(int i, byte[] bytes, int offset) {
        bytes[offset] = (byte)((i >> 24) & 0xFF);
        bytes[offset + 1] = (byte)((i >> 16) & 0xFF);
        bytes[offset + 2] = (byte)((i >> 8) & 0xFF);
        bytes[offset + 3] = (byte)(i & 0xFF);
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
