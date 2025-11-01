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
package io.seata.spring.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ScannerChecker interface compatibility wrapper.
 */
public class ScannerCheckerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ScannerChecker.class.isAnnotationPresent(Deprecated.class),
                "ScannerChecker should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(ScannerChecker.class.isInterface(), "ScannerChecker should be an interface");
    }

    @Test
    public void testExtendsApacheScannerChecker() {
        assertTrue(
                org.apache.seata.spring.annotation.ScannerChecker.class.isAssignableFrom(ScannerChecker.class),
                "ScannerChecker should extend org.apache.seata.spring.annotation.ScannerChecker");
    }

    @Test
    public void testCanBeAssignedToApacheScannerChecker() {
        Class<?>[] interfaces = ScannerChecker.class.getInterfaces();
        boolean extendsApache = false;
        for (Class<?> iface : interfaces) {
            if (iface.equals(org.apache.seata.spring.annotation.ScannerChecker.class)) {
                extendsApache = true;
                break;
            }
        }
        assertTrue(extendsApache, "ScannerChecker should directly extend Apache ScannerChecker");
    }
}
