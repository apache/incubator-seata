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
package org.apache.seata.integration.tx.api.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DubboUtilTest {

    @Test
    public void testIsDubbo3XPartialProxyName() {
        assertTrue(DubboUtil.isDubboProxyName(SimpleDubboProxy.class.getName()));
    }

    @Test
    public void testIsNotDubboProxyName() {
        assertFalse(DubboUtil.isDubboProxyName(ArrayList.class.getName()));
    }

    @Test
    public void testGetAssistInterfaceForNull() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        assertNull(DubboUtil.getAssistInterface(null));
    }

    @Test
    public void testGetAssistInterfaceForNotDubboProxy() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        assertNull(DubboUtil.getAssistInterface(new ArrayList<>()));
    }

    @Test
    public void testGetAssistInterfaceThrowsException() {
        assertThrows(NoSuchFieldException.class, () -> DubboUtil.getAssistInterface(new SimpleDubboProxy()));
    }
}
