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
package org.apache.seata.spring.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpringProxyUtilsTest {

    interface A {
    }

    interface B {
    }

    static class Base implements A {
    }

    static class Sub extends Base implements B {
    }

    @Test
    public void testIsProxy_nullAndPlainObject() {
        Assertions.assertFalse(SpringProxyUtils.isProxy(null));
        Assertions.assertFalse(SpringProxyUtils.isProxy(new Object()));
    }

    @Test
    public void testGetAllInterfaces_plainHierarchy() {
        Class<?>[] interfaces = SpringProxyUtils.getAllInterfaces(new Sub());
        // Should include A and B
        boolean hasA = false;
        boolean hasB = false;
        for (Class<?> itf : interfaces) {
            if (itf == A.class) {
                hasA = true;
            }
            if (itf == B.class) {
                hasB = true;
            }
        }
        Assertions.assertTrue(hasA);
        Assertions.assertTrue(hasB);
    }

    @Test
    public void testFindTargetClass_nonProxy() throws Exception {
        Sub sub = new Sub();
        Assertions.assertEquals(Sub.class, SpringProxyUtils.findTargetClass(sub));
    }

    @Test
    public void testGetTargetInterface_null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SpringProxyUtils.getTargetInterface(null));
    }
}
