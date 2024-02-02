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

import io.netty.channel.Channel;
import org.apache.seata.common.XID;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.rpc.RegisterCheckAuthHandler;
import org.apache.seata.discovery.registry.MultiRegistryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class NettyRemotingServerTest {

	private NettyRemotingServer nettyRemotingServer;

	@BeforeEach
	public void init() {
		nettyRemotingServer = new NettyRemotingServer(new ThreadPoolExecutor(1, 1, 0,
				TimeUnit.SECONDS, new LinkedBlockingDeque<>()));
	}
	@Test
	public void testInit() throws NoSuchFieldException, IllegalAccessException {

		MockedStatic<EnhancedServiceLoader> enhancedServiceLoaderMockedStatic = Mockito.mockStatic(EnhancedServiceLoader.class);
		enhancedServiceLoaderMockedStatic.when(() -> EnhancedServiceLoader.load((RegisterCheckAuthHandler.class))).thenReturn(null);

		MockedStatic<MultiRegistryFactory> multiRegistryFactoryMockedStatic = Mockito.mockStatic(MultiRegistryFactory.class);
		multiRegistryFactoryMockedStatic.when(MultiRegistryFactory::getInstances).thenReturn(
				Collections.emptyList());

		XID.setIpAddress("127.0.0.1");
		XID.setPort(8093);

		nettyRemotingServer.init();

		multiRegistryFactoryMockedStatic.close();
		enhancedServiceLoaderMockedStatic.close();

		Field field = NettyRemotingServer.class.getDeclaredField("initialized");
		field.setAccessible(true);

		Assertions.assertTrue(((AtomicBoolean)field.get(nettyRemotingServer)).get());
	}

	@Test
	public void testDestroyChannel() {
		Channel channel = Mockito.mock(Channel.class);
		nettyRemotingServer.destroyChannel("127.0.0.1:8091", channel);
		Mockito.verify(channel).close();
	}

	@Test
	public void destory() {
		nettyRemotingServer.destroy();
		Assertions.assertTrue(nettyRemotingServer != null);
	}
}
