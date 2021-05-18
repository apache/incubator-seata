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
package io.seata.core.context;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.model.BranchType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Root context test.
 *
 * @author guoyao
 */
public class RootContextTest {

    private final String DEFAULT_XID = "default_xid";

    private final BranchType DEFAULT_BRANCH_TYPE = BranchType.AT;

    /**
     * Test bind and unbind.
     */
    @Test
    public void testBind_And_Unbind() {
        assertThat(RootContext.unbind()).isNull();
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.unbind()).isEqualTo(DEFAULT_XID);

        RootContext.unbind();
        assertThat(RootContext.getXID()).isNull();
    }

    /**
     * Test get xid.
     */
    @Test
    public void testGetXID() {
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.unbind()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getXID()).isNull();
    }

    /**
     * Test bind and unbind branchType.
     */
    @Test
    public void testBind_And_Unbind_BranchType() {
        assertThat(RootContext.unbindBranchType()).isNull();
        RootContext.bindBranchType(DEFAULT_BRANCH_TYPE);

        //before bind xid, branchType is null
        assertThat(RootContext.getBranchType()).isNull();
        //after bind xid, branchType is not null
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(DEFAULT_BRANCH_TYPE);

        //unbind xid and branchType
        assertThat(RootContext.unbind()).isEqualTo(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isNull();
        assertThat(RootContext.unbindBranchType()).isEqualTo(DEFAULT_BRANCH_TYPE);
        assertThat(RootContext.getBranchType()).isNull();

        Assertions.assertThrows(IllegalArgumentException.class, () -> RootContext.bindBranchType(null));
    }

    /**
     * Test get branchType.
     */
    @Test
    public void testGetBranchType() {
        RootContext.bindBranchType(DEFAULT_BRANCH_TYPE);

        //before bind xid, branchType is null
        assertThat(RootContext.getBranchType()).isNull();
        //after bind xid, branchType is not null
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.getBranchType()).isEqualTo(DEFAULT_BRANCH_TYPE);

        RootContext.unbind();
        assertThat(RootContext.unbindBranchType()).isEqualTo(DEFAULT_BRANCH_TYPE);
        assertThat(RootContext.getBranchType()).isNull();
    }

    /**
     * Test in global transaction.
     */
    @Test
    public void testInGlobalTransaction() {
        assertThat(RootContext.inGlobalTransaction()).isFalse();
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.inGlobalTransaction()).isTrue();
        RootContext.unbind();
        assertThat(RootContext.inGlobalTransaction()).isFalse();
        assertThat(RootContext.getXID()).isNull();
    }

    /**
     * Test in tcc branch.
     */
    @Test
    public void testInTccBranch() {
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.inTccBranch()).isFalse();
        RootContext.bindBranchType(BranchType.TCC);
        assertThat(RootContext.inTccBranch()).isTrue();
        RootContext.unbindBranchType();
        assertThat(RootContext.inTccBranch()).isFalse();
        RootContext.unbind();
    }

    /**
     * Test in saga branch.
     */
    @Test
    public void testInSagaBranch() {
        RootContext.bind(DEFAULT_XID);
        assertThat(RootContext.inSagaBranch()).isFalse();
        RootContext.bindBranchType(BranchType.SAGA);
        assertThat(RootContext.inSagaBranch()).isTrue();
        RootContext.unbindBranchType();
        assertThat(RootContext.inSagaBranch()).isFalse();
        RootContext.unbind();
    }

    /**
     * Test assert not in global transaction with exception.
     */
    @Test
    public void testAssertNotInGlobalTransactionWithException() {
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            try {
                RootContext.assertNotInGlobalTransaction();
                RootContext.bind(DEFAULT_XID);
                RootContext.assertNotInGlobalTransaction();
            } finally {
                //clear
                RootContext.unbind();
                assertThat(RootContext.getXID()).isNull();
            }
        });
    }

    /**
     * Test assert not in global transaction.
     */
    @Test
    public void testAssertNotInGlobalTransaction() {
        RootContext.assertNotInGlobalTransaction();
        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    public void testBindBranchType_And_UnbindBranchType() {
        assertThat(RootContext.getBranchType()).isNull();
        assertThat(RootContext.unbindBranchType()).isNull();
        RootContext.bindBranchType(DEFAULT_BRANCH_TYPE);
        assertThat(RootContext.unbindBranchType()).isEqualTo(DEFAULT_BRANCH_TYPE);
        assertThat(RootContext.getBranchType()).isNull();
        assertThat(RootContext.unbindBranchType()).isNull();
    }

}
