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
package io.seata.codec.seata;


import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

/**
 * The interface Message seata codec.
 *
 * @author zhangsen
 */
public interface MessageSeataCodec {

    /**
     * Gets message type.
     *
     * @return the message type
     */
    Class<?> getMessageClassType();

    /**
     * Encode byte [ ].
     *
     * @param <T> the type parameter
     * @param t   the t
     * @param out the out
     * @return the byte [ ]
     */
    <T> void encode(T t, ByteBuf out);

    /**
     * Decode.
     *
     * @param <T> the type parameter
     * @param t   the t
     * @param in  the in
     */
    <T> void decode(T t, ByteBuffer in);
}
