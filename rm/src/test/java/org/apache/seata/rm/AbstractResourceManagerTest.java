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

import org.apache.seata.core.exception.RmTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for AbstractResourceManager
 */
public class AbstractResourceManagerTest {
    // Create anonymous implementation classes for testing
    private final AbstractResourceManager rm = new AbstractResourceManager() {
        @Override
        public BranchType getBranchType() {
            return BranchType.AT;
        }

        @Override
        public BranchStatus branchCommit(BranchType branchType, String xid, long branchId,
                                         String resourceId, String applicationData) throws TransactionException {
            throw new UnsupportedOperationException("Not implemented for test.");
        }

        @Override
        public BranchStatus branchRollback(BranchType branchType, String xid, long branchId,
                                           String resourceId, String applicationData) throws TransactionException {
            throw new UnsupportedOperationException("Not implemented for test.");
        }

        @Override
        public Map<String, Resource> getManagedResources() {
            return new HashMap<>();
        }
    };


    @Test
    void testBranchRegisterSuccess() throws Exception {
        BranchRegisterResponse mockResponse = new BranchRegisterResponse();
        mockResponse.setResultCode(ResultCode.Success);
        mockResponse.setBranchId(123L);

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenReturn(mockResponse);

            Long branchId = rm.branchRegister(BranchType.AT, "res1", "client1", "xid123", "appData", "lockKeys");
            assertEquals(123L, branchId);
        }
    }

    @Test
    void testBranchRegisterFailed() throws Exception {
        BranchRegisterResponse mockResponse = new BranchRegisterResponse();
        mockResponse.setResultCode(ResultCode.Failed);
        mockResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchRegisterFailed);
        mockResponse.setMsg("register failed");

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenReturn(mockResponse);

            assertThrows(RmTransactionException.class, () ->
                    rm.branchRegister(BranchType.AT, "res1", "client1", "xid123", "appData", "lockKeys"));
        }
    }

    @Test
    void testBranchRegisterTimeout() throws Exception {
        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenThrow(new TimeoutException("timeout"));

            RmTransactionException exception = assertThrows(RmTransactionException.class, () ->
                    rm.branchRegister(BranchType.AT, "res1", "client1", "xid123", "appData", "lockKeys"));
            assertTrue(exception.getMessage().contains("timeout"));
        }
    }

    @Test
    void testBranchReportSuccess() throws Exception {
        BranchReportResponse mockResponse = new BranchReportResponse();
        mockResponse.setResultCode(ResultCode.Success);

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenReturn(mockResponse);

            assertDoesNotThrow(() ->
                    rm.branchReport(BranchType.AT, "xid123", 100L, BranchStatus.PhaseOne_Done, "appData"));
        }
    }

    @Test
    void testBranchReportFailed() throws Exception {
        BranchReportResponse mockResponse = new BranchReportResponse();
        mockResponse.setResultCode(ResultCode.Failed);
        mockResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchReportFailed);
        mockResponse.setMsg("report failed");

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenReturn(mockResponse);

            assertThrows(RmTransactionException.class, () ->
                    rm.branchReport(BranchType.AT, "xid123", 100L, BranchStatus.PhaseOne_Failed, "appData"));
        }
    }

    @Test
    void testGetGlobalStatusSuccess() throws Exception {
        GlobalStatusResponse mockResponse = new GlobalStatusResponse();
        mockResponse.setGlobalStatus(GlobalStatus.Committed);

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenReturn(mockResponse);

            GlobalStatus status = rm.getGlobalStatus(BranchType.AT, "xid123");
            assertEquals(GlobalStatus.Committed, status);
        }
    }

    @Test
    void testGetGlobalStatusTimeout() throws Exception {
        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);
            Mockito.when(mockClient.sendSyncRequest(any())).thenThrow(new TimeoutException("timeout"));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    rm.getGlobalStatus(BranchType.AT, "xid123"));
            assertTrue(ex.getMessage().contains("timeout"));
        }
    }

    @Test
    void testUnregisterResourceShouldThrow() {
        Resource mockResource = Mockito.mock(Resource.class);
        assertThrows(RuntimeException.class, () -> rm.unregisterResource(mockResource));
    }

    @Test
    void testRegisterResource() {
        Resource mockResource = Mockito.mock(Resource.class);
        Mockito.when(mockResource.getResourceGroupId()).thenReturn("group1");
        Mockito.when(mockResource.getResourceId()).thenReturn("res1");

        try (MockedStatic<RmNettyRemotingClient> mockedStatic = Mockito.mockStatic(RmNettyRemotingClient.class)) {
            RmNettyRemotingClient mockClient = Mockito.mock(RmNettyRemotingClient.class);
            mockedStatic.when(RmNettyRemotingClient::getInstance).thenReturn(mockClient);

            assertDoesNotThrow(() -> rm.registerResource(mockResource));
            Mockito.verify(mockClient).registerResource("group1", "res1");
        }
    }

    @Test
    void testLockQueryDefaultFalse() throws TransactionException {
        assertFalse(rm.lockQuery(BranchType.AT, "resId", "xid123", "lockKeys"));
    }

}
