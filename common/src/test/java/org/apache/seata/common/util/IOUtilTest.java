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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class IOUtilTest {

    @Test
    public void testCloseWithSingleParameter() {
        FakeResource resource = new FakeResource();

        IOUtil.close(resource);

        Assertions.assertTrue(resource.isClose());
    }

    @Test
    public void testCloseWithArrayParameter() {
        FakeResource resource1 = new FakeResource();
        FakeResource resource2 = new FakeResource();

        IOUtil.close(resource1, resource2);

        Assertions.assertTrue(resource1.isClose());
        Assertions.assertTrue(resource2.isClose());
    }

    @Test
    public void testIgnoreExceptionOnClose() {
        FakeResource resource = new FakeResource() {
            @Override
            public void close() throws Exception {
                super.close();
                throw new Exception("Ops!");
            }
        };

        IOUtil.close(resource);

        Assertions.assertTrue(resource.isClose());
    }

    @Test
    public void testCloseContinuesAfterCheckedAndRuntimeExceptions() {
        CountingResource checkedFailure = new CountingResource(new IOException("Checked close failure"));
        CountingResource afterCheckedFailure = new CountingResource(null);
        CountingResource runtimeFailure = new CountingResource(new IllegalStateException("Runtime close failure"));
        CountingResource afterRuntimeFailure = new CountingResource(null);

        Assertions.assertDoesNotThrow(
                () -> IOUtil.close(checkedFailure, afterCheckedFailure, runtimeFailure, afterRuntimeFailure));

        Assertions.assertEquals(1, checkedFailure.closeCount);
        Assertions.assertEquals(1, afterCheckedFailure.closeCount);
        Assertions.assertEquals(1, runtimeFailure.closeCount);
        Assertions.assertEquals(1, afterRuntimeFailure.closeCount);
    }

    @Test
    public void testCloseSkipsNullEntries() {
        CountingResource first = new CountingResource(null);
        CountingResource second = new CountingResource(null);

        Assertions.assertDoesNotThrow(() -> IOUtil.close(null, first, null, second, null));

        Assertions.assertEquals(1, first.closeCount);
        Assertions.assertEquals(1, second.closeCount);
    }

    @Test
    public void testCloseIgnoresNullAndEmptyInputs() {
        Assertions.assertDoesNotThrow(() -> IOUtil.close((AutoCloseable) null));
        Assertions.assertDoesNotThrow(() -> IOUtil.close((AutoCloseable[]) null));
        Assertions.assertDoesNotThrow(() -> IOUtil.close(new AutoCloseable[0]));
        Assertions.assertDoesNotThrow(() -> IOUtil.close());
    }

    @Test
    public void testCloseSuppressesRuntimeExceptionAfterOneAttempt() {
        CountingResource resource = new CountingResource(new IllegalStateException("Runtime close failure"));

        Assertions.assertDoesNotThrow(() -> IOUtil.close(resource));

        Assertions.assertEquals(1, resource.closeCount);
    }

    private static class CountingResource implements AutoCloseable {

        private final Exception failure;
        private int closeCount;

        private CountingResource(Exception failure) {
            this.failure = failure;
        }

        @Override
        public void close() throws Exception {
            closeCount++;
            if (failure != null) {
                throw failure;
            }
        }
    }

    private class FakeResource implements AutoCloseable {
        private boolean close = false;

        @Override
        public void close() throws Exception {
            this.close = true;
        }

        public boolean isClose() {
            return close;
        }
    }
}
