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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.spy;

public class ShutdownHookTest {

    private int previousPriority = -1;

    private final ShutdownHook hook = ShutdownHook.getInstance();

    private final Random random = new Random();

    @BeforeAll
    static void beforeAll() {
        ShutdownHook.removeRuntimeShutdownHook();
    }

    @Test
    void testAddAndExecute() throws InterruptedException {
        // note: all of them had been added in the addDisposable method
        List<Disposable> disposableList = getRandomDisposableList();

        hook.start();
        hook.join();

        disposableList.forEach(disposable -> verify(disposable, times(1)).destroy());
    }

    private List<Disposable> getRandomDisposableList() {
        return IntStream.rangeClosed(0, 10)
                .boxed()
                .flatMap(this::generateDisposableStream)
                .collect(Collectors.toList());
    }

    private Stream<Disposable> generateDisposableStream(int priority) {
        int size = random.nextInt(10);
        return IntStream.range(0, size).mapToObj(i -> addDisposable(priority));
    }

    private Disposable addDisposable(int priority) {
        Disposable disposable = new TestDisposable(priority);
        Disposable wrapper = spy(disposable);
        hook.addDisposable(wrapper, priority);
        return wrapper;
    }

    private class TestDisposable implements Disposable {

        private final int priority;

        @Override
        public void destroy() {
            assertTrue(previousPriority <= priority, "lower priority should be executed first");
            previousPriority = priority;
        }

        public TestDisposable(int priority) {
            this.priority = priority;
        }
    }
}