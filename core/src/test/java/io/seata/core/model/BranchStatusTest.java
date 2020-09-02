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

import io.seata.common.exception.ShouldNeverHappenException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A unit test for {@link BranchStatus}
 *
 * @author Lay
 */
public class BranchStatusTest {

    private static final int REGISTERED_CODE = 1;
    private static final int NONE = 99;

    @Test
    public void testGetCode() {
        int code = BranchStatus.Registered.getCode();
        Assertions.assertEquals(code, REGISTERED_CODE);
    }

    @Test
    public void testGetWithByte() {
        BranchStatus branchStatus = BranchStatus.get((byte) REGISTERED_CODE);
        Assertions.assertEquals(branchStatus, BranchStatus.Registered);
    }

    @Test
    public void testGetWithInt() {
        BranchStatus branchStatus = BranchStatus.get(REGISTERED_CODE);
        Assertions.assertEquals(branchStatus, BranchStatus.Registered);
    }

    @Test
    public void testGetException() {
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> BranchStatus.get(NONE));
    }
}
