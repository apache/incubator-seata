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
package io.seata.common.thread;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Otis.z
 * @date 2019/2/26
 */
public class NamedThreadFactoryTest {

    @Test
    public void testNewThread() {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("testNameThread", 5);

        Thread testNameThread = namedThreadFactory
            .newThread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        assertThat(testNameThread.getName()).startsWith("testNameThread");
        assertThat(testNameThread.isDaemon()).isTrue();
    }

    @Test
    public void testConstructorWithPrefixAndDaemons() {
        NamedThreadFactory factory = new NamedThreadFactory("prefix", true);
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.getName()).startsWith("prefix");
        assertThat(thread.isDaemon()).isTrue();
    }
}
