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
package org.apache.seata.mockserver;

import org.apache.seata.common.XID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockServerLifecycleTest {
    @Test
    void preventsConcurrentInstancesAndAllowsRestartAfterClose() {
        MockServer first = new MockServer();
        MockServer second = new MockServer();
        try (MockedConstruction<MockNettyRemotingServer> transport = mockConstruction(MockNettyRemotingServer.class)) {
            try {
                first.start(10091, mock(MockCoordinator.class));
                assertThrows(IllegalStateException.class, () -> second.start(10092, mock(MockCoordinator.class)));
                assertEquals(10091, XID.getPort());
                first.close();
                second.start(10092, mock(MockCoordinator.class));
                assertEquals(10092, XID.getPort());
            } finally {
                first.close();
                second.close();
            }
            assertEquals(2, transport.constructed().size());
        }
    }
}
