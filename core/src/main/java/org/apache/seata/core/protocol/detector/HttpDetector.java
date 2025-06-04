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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;

public class HttpDetector implements ProtocolDetector {
    private static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < 8) {
            return false;
        }

        for (String method : HTTP_METHODS) {
            if (startsWith(in, method)) {
                return true;
            }
        }

        return false;
    }

    private boolean startsWith(ByteBuf buffer, String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            if (buffer.getByte(i) != (byte) prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        return new ChannelHandler[]{
            new HttpServerCodec(),
            new HttpObjectAggregator(1048576),
            new HttpDispatchHandler()
        };
    }
}