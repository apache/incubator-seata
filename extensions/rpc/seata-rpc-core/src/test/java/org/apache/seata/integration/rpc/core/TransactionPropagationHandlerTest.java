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
package org.apache.seata.integration.rpc.core;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionPropagationHandlerTest {

    private static final String DEFAULT_XID = "127.0.0.1:8091:12345678";

    @AfterEach
    void cleanup() {
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    void testGetTransactionPropagationContext_noTransaction() {
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        assertThat(context).isEmpty();
    }

    @Test
    void testGetTransactionPropagationContext_withXid() {
        RootContext.bind(DEFAULT_XID);
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        assertThat(context).containsEntry(RootContext.KEY_XID, DEFAULT_XID);
        assertThat(context).containsEntry(RootContext.KEY_BRANCH_TYPE, BranchType.AT.name());
    }

    @Test
    void testGetTransactionPropagationContext_withXidAndBranchType() {
        RootContext.bind(DEFAULT_XID);
        RootContext.bindBranchType(BranchType.TCC);
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        assertThat(context).containsEntry(RootContext.KEY_XID, DEFAULT_XID);
        assertThat(context).containsEntry(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());
    }

    @Test
    void testBindProviderContext_nullXid() {
        boolean bound = TransactionPropagationHandler.bindProviderContext(null, null);
        assertThat(bound).isFalse();
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testBindProviderContext_withXid() {
        boolean bound = TransactionPropagationHandler.bindProviderContext(DEFAULT_XID, null);
        assertThat(bound).isTrue();
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(BranchType.AT);
    }

    @Test
    void testBindProviderContext_withTCC() {
        boolean bound = TransactionPropagationHandler.bindProviderContext(DEFAULT_XID, BranchType.TCC.name());
        assertThat(bound).isTrue();
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(BranchType.TCC);
    }

    @Test
    void testBindProviderContext_withNonTCCBranchType() {
        boolean bound = TransactionPropagationHandler.bindProviderContext(DEFAULT_XID, BranchType.AT.name());
        assertThat(bound).isTrue();
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(BranchType.AT);
    }

    @Test
    void testUnbindProviderContext_normalFlow() {
        RootContext.bind(DEFAULT_XID);
        TransactionPropagationHandler.unbindProviderContext(DEFAULT_XID);
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testUnbindProviderContext_withTCCBranchType() {
        RootContext.bind(DEFAULT_XID);
        RootContext.bindBranchType(BranchType.TCC);
        TransactionPropagationHandler.unbindProviderContext(DEFAULT_XID);
        assertThat(RootContext.getXID()).isNull();
        assertThat(RootContext.getBranchType()).isNull();
    }

    @Test
    void testUnbindProviderContext_xidChanged() {
        String changedXid = "127.0.0.1:8091:99999999";
        RootContext.bind(changedXid);
        TransactionPropagationHandler.unbindProviderContext(DEFAULT_XID);
        assertThat(RootContext.getXID()).isEqualTo(changedXid);
    }

    @Test
    void testUnbindProviderContext_xidChangedWithTCC() {
        String changedXid = "127.0.0.1:8091:99999999";
        RootContext.bind(changedXid);
        RootContext.bindBranchType(BranchType.TCC);
        TransactionPropagationHandler.unbindProviderContext(DEFAULT_XID);
        assertThat(RootContext.getXID()).isEqualTo(changedXid);
        assertThat(RootContext.getBranchType()).isEqualTo(BranchType.TCC);
    }

    @Test
    void testResolveXid_xidNotNull() {
        assertThat(TransactionPropagationHandler.resolveXid("xid1", "xid2")).isEqualTo("xid1");
    }

    @Test
    void testResolveXid_xidNull() {
        assertThat(TransactionPropagationHandler.resolveXid(null, "xid2")).isEqualTo("xid2");
    }

    @Test
    void testResolveXid_bothNull() {
        assertThat(TransactionPropagationHandler.resolveXid(null, null)).isNull();
    }
}
