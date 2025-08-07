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
package org.apache.seata.rm;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultRMHandlerTest {
    @Mock
    private AbstractRMHandler mockHandlerAT;

    @Mock
    private BranchCommitRequest commitRequest;

    @Mock
    private BranchRollbackRequest rollbackRequest;

    @Mock
    private UndoLogDeleteRequest undoRequest;

    @Mock
    private BranchCommitResponse mockCommitResp;

    @Mock
    private BranchRollbackResponse mockRollbackResp;

    @BeforeEach
    void setUp() {
        DefaultRMHandler.allRMHandlersMap.clear();
        DefaultRMHandler.allRMHandlersMap.put(BranchType.AT, mockHandlerAT);
    }

    @Test
    void testHandleBranchCommitRequest() {
        when(commitRequest.getBranchType()).thenReturn(BranchType.AT);
        when(commitRequest.getXid()).thenReturn("x123");
        when(commitRequest.getBranchId()).thenReturn(456L);
        when(mockHandlerAT.handle(commitRequest)).thenReturn(mockCommitResp);

        BranchCommitResponse response = DefaultRMHandler.get().handle(commitRequest);
        assertEquals(mockCommitResp, response);

        verify(mockHandlerAT).handle(commitRequest);
    }

    @Test
    void testHandleBranchRollbackRequest() {
        when(rollbackRequest.getBranchType()).thenReturn(BranchType.AT);
        when(rollbackRequest.getXid()).thenReturn("x123");
        when(rollbackRequest.getBranchId()).thenReturn(456L);
        when(mockHandlerAT.handle(rollbackRequest)).thenReturn(mockRollbackResp);

        BranchRollbackResponse response = DefaultRMHandler.get().handle(rollbackRequest);
        assertEquals(mockRollbackResp, response);

        verify(mockHandlerAT).handle(rollbackRequest);
    }

    @Test
    void testHandleUndoLogDeleteRequest() {
        when(undoRequest.getBranchType()).thenReturn(BranchType.AT);

        DefaultRMHandler.get().handle(undoRequest);
        verify(mockHandlerAT).handle(undoRequest);
    }

    @Test
    void testGetResourceManager_shouldThrowFrameworkException() {
        DefaultRMHandler handler = (DefaultRMHandler) DefaultRMHandler.get();

        FrameworkException exception = assertThrows(FrameworkException.class, handler::getResourceManager);

        assertEquals("DefaultRMHandler isn't a real AbstractRMHandler", exception.getMessage());
    }

    @Test
    void testGetBranchType_shouldThrowFrameworkException() {
        DefaultRMHandler handler = (DefaultRMHandler) DefaultRMHandler.get();

        FrameworkException exception = assertThrows(FrameworkException.class, handler::getBranchType);

        assertEquals("DefaultRMHandler isn't a real AbstractRMHandler", exception.getMessage());
    }
}
