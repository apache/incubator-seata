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

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToRM;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.protocol.transaction.BranchRollbackRequest;
import org.apache.seata.core.protocol.transaction.BranchRollbackResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractRMHandlerTest {

    @Mock
    private ResourceManager resourceManager;

    private AbstractRMHandler rmHandler;

    @BeforeEach
    void setUp() {
        rmHandler = new AbstractRMHandler() {
            @Override
            protected ResourceManager getResourceManager() {
                return resourceManager;
            }

            @Override
            public BranchType getBranchType() {
                return BranchType.AT;
            }
        };
    }

    @Test
    void testHandleBranchCommitSuccess() throws TransactionException {
        BranchCommitRequest request = new BranchCommitRequest();
        request.setXid("xid-123");
        request.setBranchId(1L);
        request.setResourceId("res-1");
        request.setApplicationData("appData");
        request.setBranchType(BranchType.AT);

        when(resourceManager.branchCommit(any(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(BranchStatus.PhaseTwo_Committed);

        BranchCommitResponse response = rmHandler.handle(request);

        assertEquals("xid-123", response.getXid());
        assertEquals(1L, response.getBranchId());
        assertEquals(BranchStatus.PhaseTwo_Committed, response.getBranchStatus());
    }

    @Test
    void testHandleBranchRollbackSuccess() throws TransactionException {
        BranchRollbackRequest request = new BranchRollbackRequest();
        request.setXid("xid-456");
        request.setBranchId(2L);
        request.setResourceId("res-2");
        request.setApplicationData("data");
        request.setBranchType(BranchType.AT);

        when(resourceManager.branchRollback(any(), anyString(), anyLong(), anyString(), anyString()))
                .thenReturn(BranchStatus.PhaseTwo_Rollbacked);

        BranchRollbackResponse response = rmHandler.handle(request);

        assertEquals("xid-456", response.getXid());
        assertEquals(2L, response.getBranchId());
        assertEquals(BranchStatus.PhaseTwo_Rollbacked, response.getBranchStatus());
    }

    @Test
    void testOnResponseShouldLogInfo() {
        AbstractResultMessage response = mock(AbstractResultMessage.class);
        when(response.toString()).thenReturn("MockResponse");

        rmHandler.onResponse(response, new RpcContext());
        // The absence of exceptions indicates success. Log does not make assertions
    }

    @Test
    void testHandleUndoLogDelete() {
        rmHandler.handle(new UndoLogDeleteRequest());
        // This method is an empty implementation. It only needs to be verified without throwing exceptions
    }

    @Test
    void testOnRequestWithInvalidTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rmHandler.onRequest(mock(AbstractMessage.class), new RpcContext());
        });
    }

    @Test
    void testOnRequestWithValidRequest() {
        AbstractTransactionRequestToRM txRequest = mock(AbstractTransactionRequestToRM.class);
        DummyTransactionResponse dummyResponse = new DummyTransactionResponse();
        when(txRequest.handle(any())).thenReturn(dummyResponse);

        AbstractResultMessage result = rmHandler.onRequest(txRequest, new RpcContext());

        verify(txRequest).setRMInboundMessageHandler(eq(rmHandler));
        verify(txRequest).handle(any());
        assertNotNull(result);
    }

    public static class DummyTransactionResponse extends AbstractTransactionResponse {
        @Override
        public short getTypeCode() {
            return 0;
        }
    }
}

