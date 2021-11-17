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
package io.seata.server.auth;

import java.net.InetSocketAddress;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.seata.core.rpc.RegisterCheckAuthHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The type DefaultCheckAuthHandler test.
 */
@SpringBootTest
public class DefaultCheckAuthHandlerTest {

    private ChannelHandlerContext[] contexts;

    private RegisterCheckAuthHandler checkAuthHandler;

    @BeforeEach
    public void init() {
        checkAuthHandler = new DefaultCheckAuthHandler();
        contexts = new ChannelHandlerContext[3];
        Channel[] channels = new Channel[3];

        channels[0] = mock(Channel.class);
        when(channels[0].remoteAddress()).thenReturn(new InetSocketAddress("1.1.1.1", 0));
        channels[1] = mock(Channel.class);
        when(channels[1].remoteAddress()).thenReturn(new InetSocketAddress("2.2.2.2", 0));
        channels[2] = mock(Channel.class);
        when(channels[2].remoteAddress()).thenReturn(new InetSocketAddress("3.3.3.3", 0));

        for (int i = 0; i < 3; i++) {
            contexts[i] = mock(ChannelHandlerContext.class);
            when(contexts[i].channel()).thenReturn(channels[i]);
        }
    }

    @Test
    public void doRegTransactionManagerCheckTest() {
        boolean[] results = new boolean[3];
        for (int i = 0; i < 3; i++) {
            results[i] = checkAuthHandler.regTransactionManagerCheckAuth(null, contexts[i]);
        }

        Assertions.assertFalse(results[0]);
        Assertions.assertFalse(results[1]);
        Assertions.assertTrue(results[2]);
    }

    @Test
    public void doRegResourceManagerCheckTest() {
        boolean[] results = new boolean[3];
        for (int i = 0; i < 3; i++) {
            results[i] = checkAuthHandler.regResourceManagerCheckAuth(null, contexts[i]);
        }

        Assertions.assertFalse(results[0]);
        Assertions.assertFalse(results[1]);
        Assertions.assertTrue(results[2]);
    }
}
