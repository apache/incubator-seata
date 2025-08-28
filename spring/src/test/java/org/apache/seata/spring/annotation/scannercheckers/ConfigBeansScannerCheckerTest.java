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
package org.apache.seata.spring.annotation.scannercheckers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigBeansScannerCheckerTest {

    @Test
    public void testCheck_configSuffixExcluded() throws Exception {
        ConfigBeansScannerChecker checker = new ConfigBeansScannerChecker();
        Assertions.assertFalse(checker.check(new Object(), "myAppConfiguration", null));
        Assertions.assertFalse(checker.check(new Object(), "serviceProperties", null));
        Assertions.assertFalse(checker.check(new Object(), "httpClientConfig", null));
    }

    @Test
    public void testCheck_otherNamesIncluded() throws Exception {
        ConfigBeansScannerChecker checker = new ConfigBeansScannerChecker();
        Assertions.assertTrue(checker.check(new Object(), "userService", null));
    }
}


