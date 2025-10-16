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
package org.apache.seata.core.rpc.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.io.IOException;
import java.io.OutputStream;

public class ChannelOutputStreamAdapter extends OutputStream {
    private final Channel channel;
    private final byte[] singleByte = new byte[1];

    public ChannelOutputStreamAdapter(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (byte) b;
        channel.writeAndFlush(Unpooled.wrappedBuffer(singleByte));
    }

    @Override
    public void write(byte[] b) throws IOException {
        channel.writeAndFlush(Unpooled.wrappedBuffer(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        channel.writeAndFlush(Unpooled.wrappedBuffer(b, off, len));
    }

    @Override
    public void flush() throws IOException {
        channel.flush();
    }
}
