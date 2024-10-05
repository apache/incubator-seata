/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import org.apache.seata.core.rpc.netty.MultiProtocolDecoder;

public class SeataDetector implements ProtocolDetector {
    private static final byte[] MAGIC_CODE_BYTES = {(byte) 0xda, (byte) 0xda};
    private ChannelHandler[] serverHandlers;

    public SeataDetector(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < MAGIC_CODE_BYTES.length) {
            return false;
        }
        for (int i = 0; i < MAGIC_CODE_BYTES.length; i++) {
            if (in.getByte(i) != MAGIC_CODE_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        MultiProtocolDecoder multiProtocolDecoder = new MultiProtocolDecoder(serverHandlers);

        return new ChannelHandler[]{multiProtocolDecoder};
    }
}