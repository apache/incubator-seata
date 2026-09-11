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
package io.seata.server.event;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.RemotingServer;
import io.seata.metrics.registry.Registry;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.coordinator.DefaultCoordinatorTest;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.metrics.MetricsManager;
import io.seata.server.session.SessionHolder;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.StoreConfig.SessionMode;
import io.seata.server.util.StoreUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Test events come from Default Core.
 *
 * @author zhengyangyong
 */
@SpringBootTest
public class DefaultCoreForEventBusTest {

    private static final boolean DELAY_HANDLE_SESSION = StoreConfig.getSessionMode() != SessionMode.FILE;

    @BeforeAll
    public static void setUp(ApplicationContext context) throws InterruptedException {
        StoreUtil.deleteDataFile();
        Thread.sleep(5000);
    }

    @Test
    public void test() throws IOException, TransactionException, InterruptedException {
        class GlobalTransactionEventSubscriber {
            private final Map<String, AtomicInteger> eventCounters;
            private CountDownLatch downLatch;

            public Map<String, AtomicInteger> getEventCounters() {
                return eventCounters;
            }

            public GlobalTransactionEventSubscriber() {
                this.eventCounters = new ConcurrentHashMap<>();
            }

            @Subscribe
            @AllowConcurrentEvents
            public void processGlobalTransactionEvent(GlobalTransactionEvent event) {
                AtomicInteger counter = eventCounters.computeIfAbsent(event.getStatus(),
                        status -> new AtomicInteger(0));
                counter.addAndGet(1);
                //System.out.println("current status:" + event.getName() + "," + event.getStatus() + "," + eventCounters.size());
                if (null != downLatch) {
                    downLatch.countDown();
                }
            }

            public void setDownLatch(CountDownLatch countDownLatch) {
                this.downLatch = countDownLatch;
            }

            public CountDownLatch getDownLatch() {
                return downLatch;
            }

            public void resetDownLatch() {
                if (null != downLatch) {
                    downLatch = null;
                }
            }
        }
        RemotingServer remotingServer = new DefaultCoordinatorTest.MockServerMessageSender();
        DefaultCoordinator coordinator = DefaultCoordinator.getInstance(remotingServer);
        coordinator.init();
        GlobalTransactionEventSubscriber subscriber = null;
        try {
            DefaultCore core = new DefaultCore(remotingServer);
            SessionHolder.init(null);
            subscriber = new GlobalTransactionEventSubscriber();
            EventBusManager.get().unregisterAll();
            EventBusManager.get().register(subscriber);

            //start and commit a transaction
            subscriber.setDownLatch(new CountDownLatch(DELAY_HANDLE_SESSION ? 3 : 4));
            String xid = core.begin("test_app_id", "default_group", "test_tran_name", 30000);
            core.commit(xid);


            //we need sleep for a short while because default canBeCommittedAsync() is true
            subscriber.getDownLatch().await();
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Begin.name()).get());
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.AsyncCommitting.name()).get());
            // after event and sync event
            Assertions.assertEquals(DELAY_HANDLE_SESSION ? 1 : 2,
                subscriber.getEventCounters().get(GlobalStatus.Committed.name()).get());

            //start and rollback transaction
            subscriber.setDownLatch(new CountDownLatch(3));
            xid = core.begin("test_app_id", "default_group", "test_tran_name2", 30000);
            core.rollback(xid);
            //sleep for retryRollback
            Thread.sleep(1500);
            //check
            subscriber.getDownLatch().await();
            Assertions.assertEquals(2, subscriber.getEventCounters().get(GlobalStatus.Begin.name()).get());
            //Because of the delayed deletion of GlobalSession, and without changing the status of the Session,
            Assertions.assertEquals(1, subscriber.getEventCounters().get(GlobalStatus.Rollbacking.name()).get());
            Assertions.assertNotNull(subscriber.getEventCounters().get(GlobalStatus.Rollbacked.name()));

            //start more one new transaction for test timeout and let this transaction immediately timeout
            subscriber.setDownLatch(new CountDownLatch(1));
            core.begin("test_app_id", "default_group", "test_tran_name3", 0);

            //sleep for check ->  DefaultCoordinator.timeoutCheck
            Thread.sleep(2000);

            //at lease retry once because DefaultCoordinator.timeoutCheck is 1 second
            subscriber.downLatch.await(5000, TimeUnit.MILLISECONDS);
            Assertions.assertTrue(subscriber.getEventCounters().get(GlobalStatus.TimeoutRollbacking.name()).get() >= 1);
        } finally {
            // call SpringContextShutdownHook
            if (null != subscriber) {
                EventBusManager.get().unregister(subscriber);
            }
        }
    }

    @AfterAll
    public static void setDown() throws InterruptedException {
        Optional.ofNullable(DefaultCoordinator.getInstance()).ifPresent(DefaultCoordinator::destroy);
        Optional.ofNullable(MetricsManager.get().getRegistry()).ifPresent(Registry::clearUp);
    }

}
