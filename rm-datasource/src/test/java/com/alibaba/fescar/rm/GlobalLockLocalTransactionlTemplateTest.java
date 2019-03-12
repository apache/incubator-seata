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
package com.alibaba.fescar.rm;

import java.util.concurrent.Callable;

import com.alibaba.fescar.core.context.RootContext;

import org.junit.Assert;
import org.junit.Test;

/**
 * check GlobalLockLocalTransactionlTemplate
 *
 * @author deyou
 */
public class GlobalLockLocalTransactionlTemplateTest {

    /**
     * Test sql recognizing.
     *
     * @throws Exception
     */
    @Test
    public void testFlag() throws Exception {

        GlobalLockTemplate<Object> template = new GlobalLockTemplate<Object>();

        template.execute(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                Assert.assertTrue("lock flag not set!", RootContext.requireGlobalLock());
                return null;
            }
        });

        Assert.assertTrue("lock flag not clean!", !RootContext.requireGlobalLock());

    }
}
