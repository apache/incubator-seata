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
package org.apache.seata.common.http;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class HttpExecutorFactoryTest {
    @Test
    void testGetInstance_singleton() {
        HttpExecutor mockExecutor = mock(HttpExecutor.class);

        try (MockedStatic<EnhancedServiceLoader> loaderMock = mockStatic(EnhancedServiceLoader.class)) {
            loaderMock
                    .when(() -> EnhancedServiceLoader.load(HttpExecutor.class, "Http1"))
                    .thenReturn(mockExecutor);

            HttpExecutor first = HttpExecutorFactory.getInstance();
            HttpExecutor second = HttpExecutorFactory.getInstance();

            assertEquals(first, second);
        }
    }
}
