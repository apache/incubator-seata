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
package org.apache.seata.tm;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * the type DefaultTransactionManager
 */
public class DefaultTransactionManagerTest {

    private final static String DEFAULT_XID = "1234567890";

    private DefaultTransactionManager defaultTransactionManager;

    private MockedStatic<TmNettyRemotingClient> tmNettyRemotingClientMockedStatic;

    @Mock
    private TmNettyRemotingClient tmNettyRemotingClient;


    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        tmNettyRemotingClientMockedStatic = Mockito.mockStatic(TmNettyRemotingClient.class);
        tmNettyRemotingClientMockedStatic.when(TmNettyRemotingClient::getInstance).thenReturn(tmNettyRemotingClient);
        defaultTransactionManager = new DefaultTransactionManager();
    }

    @AfterEach
    void destory(){
        tmNettyRemotingClientMockedStatic.close();
    }

    @Test
    void testBeginSuccess() throws Exception {
        GlobalBeginResponse mockResponse = new GlobalBeginResponse();
        mockResponse.setResultCode(ResultCode.Success);
        mockResponse.setXid(DEFAULT_XID);

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalBeginRequest.class))).thenReturn(mockResponse);

        String xid = defaultTransactionManager.begin("appId", "txGroup", "testName", 1000);

        Assertions.assertEquals("1234567890", xid);
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalBeginRequest.class));
    }

    @Test
    void testBeginFailure() throws Exception {
        GlobalBeginResponse mockResponse = new GlobalBeginResponse();
        mockResponse.setResultCode(ResultCode.Failed);
        mockResponse.setMsg("Failed to begin transaction");

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalBeginRequest.class))).thenReturn(mockResponse);

        TransactionException exception = Assertions.assertThrows(TransactionException.class,
                () -> defaultTransactionManager.begin("appId", "txGroup", "testName", 1000));

        Assertions.assertTrue(exception.getMessage().contains("Failed to begin transaction"));
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalBeginRequest.class));
    }

    @Test
    void testCommitSuccess() throws Exception {
        GlobalCommitResponse mockResponse = new GlobalCommitResponse();
        mockResponse.setGlobalStatus(GlobalStatus.Committed);

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalCommitRequest.class))).thenReturn(mockResponse);

        GlobalStatus status = defaultTransactionManager.commit(DEFAULT_XID);

        Assertions.assertEquals(GlobalStatus.Committed, status);
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalCommitRequest.class));
    }

    @Test
    void testRollbackSuccess() throws Exception {
        GlobalRollbackResponse mockResponse = new GlobalRollbackResponse();
        mockResponse.setGlobalStatus(GlobalStatus.Rollbacked);

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalRollbackRequest.class))).thenReturn(mockResponse);

        GlobalStatus status = defaultTransactionManager.rollback(DEFAULT_XID);

        Assertions.assertEquals(GlobalStatus.Rollbacked, status);
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalRollbackRequest.class));
    }

    @Test
    void testGetStatusSuccess() throws Exception {
        GlobalStatusResponse mockResponse = new GlobalStatusResponse();
        mockResponse.setGlobalStatus(GlobalStatus.Committing);

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalStatusRequest.class))).thenReturn(mockResponse);

        GlobalStatus status = defaultTransactionManager.getStatus(DEFAULT_XID);

        Assertions.assertEquals(GlobalStatus.Committing, status);
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalStatusRequest.class));
    }

    @Test
    void testGlobalReportSuccess() throws Exception {
        GlobalReportResponse mockResponse = new GlobalReportResponse();
        mockResponse.setGlobalStatus(GlobalStatus.Committed);

        when(tmNettyRemotingClient.sendSyncRequest(any(GlobalReportRequest.class))).thenReturn(mockResponse);

        GlobalStatus status = defaultTransactionManager.globalReport(DEFAULT_XID, GlobalStatus.Committed);

        Assertions.assertEquals(GlobalStatus.Committed, status);
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(GlobalReportRequest.class));
    }

    @Test
    void testSyncCallTimeout() throws Exception {
        when(tmNettyRemotingClient.sendSyncRequest(any(AbstractTransactionRequest.class)))
                .thenThrow(new TimeoutException("Timeout occurred"));

        TransactionException exception = Assertions.assertThrows(TransactionException.class,
                () -> defaultTransactionManager.getStatus(DEFAULT_XID));

        Assertions.assertTrue(exception.getMessage().contains("RPC timeout"));
        Mockito.verify(tmNettyRemotingClient).sendSyncRequest(any(AbstractTransactionRequest.class));
    }
}
