/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * A unit test for {@link GlobalStatus}
 * @author Lay
 * @date 2019/3/6
 */
public class GlobalStatusTest {
    private static final int BEGIN_CODE = 1;
    private static final int NONE = 99;

    @Test
    public void testGetCode() {
        int code = GlobalStatus.Begin.getCode();
        Assert.assertEquals(code, BEGIN_CODE);
    }

    @Test
    public void testGetWithByte() {
        GlobalStatus branchStatus = GlobalStatus.get((byte)BEGIN_CODE);
        Assert.assertEquals(branchStatus, GlobalStatus.Begin);
    }

    @Test
    public void testGetWithInt() {
        GlobalStatus branchStatus = GlobalStatus.get(BEGIN_CODE);
        Assert.assertEquals(branchStatus, GlobalStatus.Begin);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetException() {
        GlobalStatus.get(NONE);
    }

}
