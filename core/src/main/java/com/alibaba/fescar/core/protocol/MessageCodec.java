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

/**
 * The interface Message codec.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/14
 */
public interface MessageCodec {
    /**
     * Gets type code.
     *
     * @return the type code
     */
    short getTypeCode();

    /**
     * Encode byte [ ].
     *
     * @return the byte [ ]
     */
    byte[] encode();

    /**
     * Decode boolean.
     *
     * @param in the in
     * @return the boolean
     */
    boolean decode(ByteBuf in);
}
