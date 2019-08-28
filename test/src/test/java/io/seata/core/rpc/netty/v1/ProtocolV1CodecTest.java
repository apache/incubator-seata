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
package io.seata.core.rpc.netty.v1;

import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class ProtocolV1CodecTest {

    /**
     * Logger for ProtocolV1CodecTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV1CodecTest.class);

    @Test
    public void testAll() {
        ProtocolV1Server server = new ProtocolV1Server();
        ProtocolV1Client client = new ProtocolV1Client();
        try {
            server.start();
            client.connect("127.0.0.1", 8811, 500);

            Assertions.assertTrue(client.channel.isActive());

            Map<String, String> head = new HashMap<>();
            head.put("tracerId", "xxadadadada");
            head.put("token", "adadadad");
            head.put("hello", null);

            BranchCommitRequest body = new BranchCommitRequest();
            body.setBranchId(12345L);
            body.setApplicationData("application");
            body.setBranchType(BranchType.AT);
            body.setResourceId("resource-1234");
            body.setXid("xid-1234");

            // test run times
            int runTimes = 100000;

            final int threads = 50;
            final CountDownLatch cnt = new CountDownLatch(runTimes);
            final AtomicInteger tag = new AtomicInteger(0);
            final AtomicInteger success = new AtomicInteger(0);
            // no queue
            final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                    new SynchronousQueue<>(), new NamedThreadFactory("client-", false));
            for (int i = 0; i < threads; i++) {
                service1.execute(() -> {
                    while (tag.getAndIncrement() < runTimes) {
                        try {
                            Future future = client.sendRpc(head, body);
                            RpcMessage resp = (RpcMessage) future.get(10, TimeUnit.SECONDS);
                            if (resp != null) {
                                success.incrementAndGet();
                            }
                        } catch (Exception e) {
                            LOGGER.error("Client send error", e);
                        } finally {
                            cnt.countDown();
                        }
                    }
                });
            }

            cnt.await();
            LOGGER.info("success {}/{}", success.get(), runTimes);
            Assertions.assertEquals(success.get(), runTimes);
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
        } finally {
            client.close();
            server.stop();
        }
    }
}
