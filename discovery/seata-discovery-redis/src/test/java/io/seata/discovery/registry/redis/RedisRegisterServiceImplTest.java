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
package io.seata.discovery.registry.redis;

import com.github.microwww.redis.RedisServer;
import io.seata.common.util.NetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author laywin
 */
public class RedisRegisterServiceImplTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testRemoveServerAddressByPushEmptyProtection() {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "default");
        System.setProperty("registry.redis.serverAddr", "127.0.0.1:6789");
        System.setProperty("registry.redis.cluster", "default");
        RedisServer server = new RedisServer();
        RedisRegistryServiceImpl redisRegistryService = null;
        try {
            server.listener("127.0.0.1", 6789);
            redisRegistryService = RedisRegistryServiceImpl.getInstance();
            logger.info("before time: {}", System.currentTimeMillis());
            redisRegistryService.lookup("default_tx_group");
            redisRegistryService.register(new InetSocketAddress(NetUtil.getLocalIp(), 8091));
            redisRegistryService.register(new InetSocketAddress(NetUtil.getLocalIp(), 8092));
            List<InetSocketAddress> list = redisRegistryService.lookup("default_tx_group");
            Assertions.assertEquals(2, list.size());
            redisRegistryService.unregister(new InetSocketAddress(NetUtil.getLocalIp(), 8091));
            redisRegistryService.unregister(new InetSocketAddress(NetUtil.getLocalIp(), 8092));
            list = redisRegistryService.lookup("default_tx_group");
            logger.info("after time: {}", System.currentTimeMillis());
            Assertions.assertEquals(1, list.size());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (redisRegistryService != null) {
                redisRegistryService.close();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
