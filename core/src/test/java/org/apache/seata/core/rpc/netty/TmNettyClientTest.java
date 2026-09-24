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
package org.apache.seata.core.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ServerVersionHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Tm rpc client test.
 */
@Order(1)
public class TmNettyClientTest {

    /**
     * Test get instance.
     *
     * @throws Exception the exceptionDataSourceManager.
     */
    @Test
    public void testGetInstance() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "group A";
        TmNettyRemotingClient tmNettyRemotingClient =
                TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        Field nettyClientKeyPoolField =
                getDeclaredField(tmNettyRemotingClient.getClientChannelManager(), "nettyClientKeyPool");
        nettyClientKeyPoolField.setAccessible(true);
        GenericKeyedObjectPool nettyClientKeyPool =
                (GenericKeyedObjectPool) nettyClientKeyPoolField.get(tmNettyRemotingClient.getClientChannelManager());
        NettyClientConfig defaultNettyClientConfig = new NettyClientConfig();
        Assertions.assertEquals(defaultNettyClientConfig.getMaxPoolActive(), nettyClientKeyPool.getMaxActive());
        Assertions.assertEquals(defaultNettyClientConfig.getMinPoolIdle(), nettyClientKeyPool.getMinIdle());
        Assertions.assertEquals(defaultNettyClientConfig.getMaxAcquireConnMills(), nettyClientKeyPool.getMaxWait());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolTestBorrow(), nettyClientKeyPool.getTestOnBorrow());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolTestReturn(), nettyClientKeyPool.getTestOnReturn());
        Assertions.assertEquals(defaultNettyClientConfig.isPoolLifo(), nettyClientKeyPool.getLifo());
    }

    /**
     * Do connect.
     *
     * @throws Exception the exception
     */
    @Test
    public void testInit() throws Exception {
        String applicationId = "app 1";
        String transactionServiceGroup = "default_tx_group";
        TmNettyRemotingClient tmNettyRemotingClient =
                TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        System.setProperty(ConfigurationKeys.ENABLE_RM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "false");
        ConfigurationCache.clear();
        tmNettyRemotingClient.init();
        // check if attr of tmNettyClient object has been set success
        Field clientBootstrapField = getDeclaredField(tmNettyRemotingClient, "clientBootstrap");
        clientBootstrapField.setAccessible(true);
        NettyClientBootstrap clientBootstrap = (NettyClientBootstrap) clientBootstrapField.get(tmNettyRemotingClient);
        Field bootstrapField = getDeclaredField(clientBootstrap, "bootstrap");
        bootstrapField.setAccessible(true);
        Bootstrap bootstrap = (Bootstrap) bootstrapField.get(clientBootstrap);

        Assertions.assertNotNull(bootstrap);
        Field optionsField = getDeclaredField(bootstrap, "options");
        optionsField.setAccessible(true);
        Map<ChannelOption<?>, Object> options = (Map<ChannelOption<?>, Object>) optionsField.get(bootstrap);
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.TCP_NODELAY));
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.SO_KEEPALIVE));
        Assertions.assertEquals(10000, options.get(ChannelOption.CONNECT_TIMEOUT_MILLIS));
        Assertions.assertEquals(Boolean.TRUE, options.get(ChannelOption.SO_KEEPALIVE));
        Assertions.assertEquals(153600, options.get(ChannelOption.SO_RCVBUF));

        Field channelFactoryField = getDeclaredField(bootstrap, "channelFactory");
        channelFactoryField.setAccessible(true);
        ChannelFactory<? extends Channel> channelFactory =
                (ChannelFactory<? extends Channel>) channelFactoryField.get(bootstrap);
        Assertions.assertNotNull(channelFactory);

        if (Epoll.isAvailable()) {
            Assertions.assertTrue(channelFactory.newChannel() instanceof EpollSocketChannel);
        } else {
            Assertions.assertTrue(channelFactory.newChannel() instanceof NioSocketChannel);
        }
    }

    /**
     * Gets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void getApplicationId() throws Exception {}

    /**
     * Sets application id.
     *
     * @throws Exception the exception
     */
    @Test
    public void setApplicationId() throws Exception {}

    @AfterAll
    public static void afterAll() {
        TmNettyRemotingClient.getInstance().destroy();
        System.setProperty(ConfigurationKeys.ENABLE_TM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "false");
    }

    @Test
    public void testCheckFailFast() throws Exception {
        TmNettyRemotingClient.getInstance().destroy();
        TmNettyRemotingClient tmClient = TmNettyRemotingClient.getInstance("fail_fast", "default_tx_group");
        System.setProperty("file.listener.enabled", "true");
        System.setProperty(ConfigurationKeys.ENABLE_TM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "true");
        ConfigurationCache.clear();
        Assertions.assertThrows(FrameworkException.class, tmClient::init);
        System.setProperty(ConfigurationKeys.ENABLE_TM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "false");
    }

    @Test
    public void onRegisterMsgSuccessRecordsServerVersionTest() throws Exception {
        ServerVersionHolder.clear();
        TmNettyRemotingClient.getInstance().destroy();
        TmNettyRemotingClient tmClient = TmNettyRemotingClient.getInstance("app", "default_tx_group");

        String serverAddress = "127.0.0.1:8091";
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        RegisterTMRequest request = new RegisterTMRequest("app", "default_tx_group");
        RegisterTMResponse response = new RegisterTMResponse();
        response.setVersion("2.6.0");

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        Field field = AbstractNettyRemotingClient.class.getDeclaredField("clientChannelManager");
        field.setAccessible(true);
        field.set(tmClient, channelManager);

        try {
            tmClient.onRegisterMsgSuccess(serverAddress, channel, response, request);

            // the version carried by the register channel is the peer(server) version
            verify(channelManager).registerChannel(eq(serverAddress), eq(channel), eq("2.6.0"));
            Assertions.assertEquals("2.6.0", ServerVersionHolder.getServerVersion(serverAddress));
        } finally {
            ServerVersionHolder.clear();
            TmNettyRemotingClient.getInstance().destroy();
        }
    }

    /**
     * get private field in parent class
     *
     * @param object    the object
     * @param fieldName the field name
     * @return declared field
     */
    public static Field getDeclaredField(Object object, String fieldName) {
        Field field = null;
        Class<?> clazz = object.getClass();
        for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName);
                return field;
            } catch (Exception e) {

            }
        }

        return null;
    }
}
