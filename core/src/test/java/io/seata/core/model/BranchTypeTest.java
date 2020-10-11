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
package io.seata.core.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A unit test for {@link BranchType}
 *
 * @author Lay
 */
public class BranchTypeTest {

    private static final int AT_ORDINAL = 0;
    private static final int NONE = 99;

    @Test
    public void testOrdinal() {
        int ordinal = BranchType.AT.ordinal();
        Assertions.assertEquals(AT_ORDINAL, ordinal);
    }

    @Test
    public void testGetWithOrdinal() {
        BranchType type = BranchType.get(BranchType.AT.ordinal());
        Assertions.assertEquals(type, BranchType.AT);
    }

    @Test
    public void testGetWithByte() {
        BranchType branchStatus = BranchType.get((byte) AT_ORDINAL);
        Assertions.assertEquals(branchStatus, BranchType.AT);
    }

    @Test
    public void testGetWithInt() {
        BranchType branchStatus = BranchType.get(AT_ORDINAL);
        Assertions.assertEquals(branchStatus, BranchType.AT);
    }

    @Test
    public void testGetException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BranchType.get(NONE));
    }


}
