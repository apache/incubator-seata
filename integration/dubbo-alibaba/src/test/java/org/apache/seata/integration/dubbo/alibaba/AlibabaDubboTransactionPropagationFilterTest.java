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
package org.apache.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.rpc.RpcContext;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.dubbo.alibaba.mock.MockInvoker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class AlibabaDubboTransactionPropagationFilterTest {

    private static final String DEFAULT_XID = "1234567890";

    @Test
    public void testInvoke_And_RootContext() {
        AlibabaDubboTransactionPropagationFilter filter = new AlibabaDubboTransactionPropagationFilter();

        // SAGA
        RpcContext.getContext().setAttachment(RootContext.KEY_XID, DEFAULT_XID);
        RpcContext.getContext().setAttachment(RootContext.KEY_BRANCH_TYPE, BranchType.SAGA.name());
        filter.invoke(new MockInvoker(() -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(RootContext.getBranchType()).isEqualTo(BranchType.AT);
        }), null);
        assertThat(RootContext.unbind()).isNull();
        assertThat(RootContext.unbindBranchType()).isNull();

        // TCC
        RpcContext.getContext().setAttachment(RootContext.KEY_XID, DEFAULT_XID);
        RpcContext.getContext().setAttachment(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());
        filter.invoke(new MockInvoker(() -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(RootContext.getBranchType()).isEqualTo(BranchType.TCC);
        }), null);
        assertThat(RootContext.unbind()).isNull();
        assertThat(RootContext.unbindBranchType()).isNull();

        // TCC
        RootContext.bind(DEFAULT_XID);
        RootContext.bindBranchType(BranchType.SAGA);
        RpcContext.getContext().setAttachment(RootContext.KEY_XID, DEFAULT_XID);
        RpcContext.getContext().setAttachment(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());
        filter.invoke(new MockInvoker(() -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(RootContext.getBranchType()).isEqualTo(BranchType.SAGA);
        }), null);
        assertThat(RootContext.unbind()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.unbindBranchType()).isEqualTo(BranchType.SAGA);
    }
}
