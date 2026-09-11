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
package io.seata.core.rpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.netty.RpcServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  IDENTIFIED_CHANNELS Channel FullGC OOM
 *  @author alex-ashuo
 */
public class ChannelManagerOOMTest {

    private static final String APP_ID = "business-app";
    private static final String TX_GROUP = "default_tx_group";
    private static final String RESOURCE_ID = "jdbc:mysql://127.0.0.1:3306/seata";

    @AfterEach
    public void tearDown() throws Exception {
        clearStaticMap("IDENTIFIED_CHANNELS");
        clearStaticMap("TM_CHANNELS");
        clearStaticMap("RM_CHANNELS");
    }

    /**
     * Mock K8s Pod TM RM IDENTIFIED_CHANNELS FullGC OOM
     */
    @Test
    public void repeatedClientReconnectChannels() throws Exception {
        int iterations = 200;
        for (int i = 0; i < iterations; i++) {
            //TM channel
            Channel tmChannel = newMockDeadChannel(20000 + i);
            ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), tmChannel);
            ChannelManager.getContextFromIdentified(tmChannel).release();

            //RM channel
            Channel rmChannel = newMockDeadChannel(30000 + i);
            RegisterRMRequest rmRequest = new RegisterRMRequest(APP_ID, TX_GROUP);
            rmRequest.setResourceIds(RESOURCE_ID);
            ChannelManager.registerRMChannel(rmRequest, rmChannel);
            ChannelManager.getContextFromIdentified(rmChannel).release();
        }

        int identifiedSize = staticMap("IDENTIFIED_CHANNELS").size();
        assertEquals(0, identifiedSize);
    }

    /**
     * TM RpcContext.release()
     */
    @Test
    public void tmChannelRelease() throws Exception {
        Channel channel = newMockDeadChannel(10001);
        RegisterTMRequest request = new RegisterTMRequest(APP_ID, TX_GROUP);

        //RpcServer.onRegTmMessage → ChannelManager.registerTMChannel
        ChannelManager.registerTMChannel(request, channel);
        assertTrue(ChannelManager.isRegistered(channel));

        // RpcServer rpcContext.release()
        ChannelManager.getContextFromIdentified(channel).release();
        assertFalse(ChannelManager.isRegistered(channel));
    }

    /**
     * RM RpcContext.release()
     */
    @Test
    public void rmChannelRelease() throws Exception {
        Channel channel = newMockDeadChannel(10002);
        RegisterRMRequest request = new RegisterRMRequest(APP_ID, TX_GROUP);
        request.setResourceIds(RESOURCE_ID);

        ChannelManager.registerRMChannel(request, channel);
        assertTrue(ChannelManager.isRegistered(channel));

        // RpcServer rpcContext.release()
        ChannelManager.getContextFromIdentified(channel).release();
        assertFalse(ChannelManager.isRegistered(channel));
    }

    /**
     * ChannelManager.releaseRpcContext
     */
    @Test
    public void releaseRpcContextChannels() throws Exception {
        Channel channel = newMockDeadChannel(40001);
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);
        assertTrue(ChannelManager.isRegistered(channel));

        ChannelManager.releaseRpcContext(channel);
        assertFalse(ChannelManager.isRegistered(channel));
    }

    /**
     * TM RpcServer#handleDisconnect(ChannelHandlerContext)
     */
    @Test
    public void rpcServerHandleDisconnectTmChannels() throws Exception {
        Channel channel = newMockDeadChannel(60001);

        // register TM Channel
        ChannelManager.registerTMChannel(new RegisterTMRequest(APP_ID, TX_GROUP), channel);
        assertTrue(ChannelManager.isRegistered(channel));

        // mock ChannelHandlerContext handleDisconnect ctx.channel().remoteAddress()
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(ctx.channel()).thenReturn(channel);

        // RpcServer.handleDisconnect
        Method handleDisconnect = RpcServer.class.getDeclaredMethod("handleDisconnect",
            ChannelHandlerContext.class);
        handleDisconnect.setAccessible(true);
        handleDisconnect.invoke(new RpcServer(Mockito.mock(ThreadPoolExecutor.class)), ctx);

        assertFalse(ChannelManager.isRegistered(channel));
        assertNull(ChannelManager.getContextFromIdentified(channel));
    }

    /**
     * RM RpcServer#handleDisconnect(ChannelHandlerContext)
     */
    @Test
    public void rpcServerHandleDisconnectRmChannels() throws Exception {
        Channel channel = newMockDeadChannel(60002);
        RegisterRMRequest rmRequest = new RegisterRMRequest(APP_ID, TX_GROUP);
        rmRequest.setResourceIds(RESOURCE_ID);

        // register RM Channel
        ChannelManager.registerRMChannel(rmRequest, channel);
        assertTrue(ChannelManager.isRegistered(channel));

        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Mockito.when(ctx.channel()).thenReturn(channel);

        Method handleDisconnect = RpcServer.class.getDeclaredMethod("handleDisconnect",
            ChannelHandlerContext.class);
        handleDisconnect.setAccessible(true);
        handleDisconnect.invoke(new RpcServer(Mockito.mock(ThreadPoolExecutor.class)), ctx);

        assertFalse(ChannelManager.isRegistered(channel));
    }

    /**
     * Mock remoteAddress K8s Pod IP:Port。
     */
    private static Channel newMockDeadChannel(int remotePort) {
        Channel channel = Mockito.mock(Channel.class);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", remotePort);
        Mockito.when(channel.remoteAddress()).thenReturn(remoteAddress);
        Mockito.when(channel.isActive()).thenReturn(false);
        return channel;
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Object, Object> staticMap(String fieldName) throws Exception {
        Field field = ChannelManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (ConcurrentMap<Object, Object>) field.get(null);
    }

    private static void clearStaticMap(String fieldName) throws Exception {
        staticMap(fieldName).clear();
    }
}
