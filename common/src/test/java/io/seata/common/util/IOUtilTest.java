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
package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author caioguedes
 */
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

    private class FakeResource implements AutoCloseable{
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