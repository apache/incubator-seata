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
package com.alibaba.fescar.core.context;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by guoyao on 2019/2/28.
 */
public class RootContextTest {

    private final String DEFAULT_XID="default_xid";

    @Test
    public void testBind_And_Unbind() {
        Assert.assertNull(RootContext.unbind());
        RootContext.bind(DEFAULT_XID);
        Assert.assertEquals(DEFAULT_XID, RootContext.unbind());
    }

    @Test
    public void testGetXID() {
        RootContext.bind(DEFAULT_XID);
        Assert.assertEquals(DEFAULT_XID, RootContext.getXID());
        Assert.assertEquals(DEFAULT_XID, RootContext.unbind());
        Assert.assertNull(RootContext.getXID());
    }

    @Test
    public void testInGlobalTransaction() {
        Assert.assertTrue(!RootContext.inGlobalTransaction());
        RootContext.bind(DEFAULT_XID);
        Assert.assertTrue(RootContext.inGlobalTransaction());
        RootContext.unbind();
        Assert.assertTrue(!RootContext.inGlobalTransaction());
    }

    @Test(expected =ShouldNeverHappenException.class)
    public void testAssertNotInGlobalTransactionWithException() {
        RootContext.assertNotInGlobalTransaction();
        RootContext.bind(DEFAULT_XID);
        RootContext.assertNotInGlobalTransaction();
    }

    @Test
    public void testAssertNotInGlobalTransaction() {
        RootContext.assertNotInGlobalTransaction();
    }

}
