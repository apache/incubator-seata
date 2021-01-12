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
package io.seata.core.event;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test default GuavaEventBus.
 *
 * @author zhengyangyong
 */
public class GuavaEventBusTest {
    @Test
    public void test() {

        AtomicInteger counter = new AtomicInteger(0);
        EventBus eventBus = new GuavaEventBus("test");

        class TestEvent implements Event {
            private final int value;

            public int getValue() {
                return value;
            }

            public TestEvent(int value) {
                this.value = value;
            }
        }

        class TestSubscriber {
            @Subscribe
            public void process(TestEvent event) {
                counter.addAndGet(event.getValue());
            }
        }

        TestSubscriber subscriber = new TestSubscriber();
        eventBus.register(subscriber);

        eventBus.post(new TestEvent(1));

        Assertions.assertEquals(1, counter.get());

        eventBus.unregister(subscriber);

        eventBus.post(new TestEvent(1));

        Assertions.assertEquals(1, counter.get());
    }
}
