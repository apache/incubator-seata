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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.stream.Stream;

/**
 * Test for {@link ConfigBeansScannerChecker} class
 */
public class ConfigBeansScannerCheckerTest {

    private final ConfigBeansScannerChecker checker = new ConfigBeansScannerChecker();
    private final Object testBean = new Object();
    private final ConfigurableListableBeanFactory beanFactory = null;

    /**
     * Provide test data
     */
    private static Stream<Arguments> provideBeanNameTestCases() {
        return Stream.of(
                // Bean names ending with Configuration should return false
                Arguments.of("testConfiguration", false),
                // Bean names ending with Properties should return false
                Arguments.of("testProperties", false),
                // Bean names ending with Config should return false
                Arguments.of("testConfig", false),
                // Normal bean names should return true
                Arguments.of("normalBean", true),
                // Empty bean name should return true
                Arguments.of("", true),
                // Bean names containing but not ending with Configuration should return true
                Arguments.of("ConfigurationTest", true),
                // Bean names containing but not ending with Properties should return true
                Arguments.of("PropertiesTest", true),
                // Bean names containing but not ending with Config should return true
                Arguments.of("ConfigTest", true));
    }

    /**
     * Helper method: execute checker.check and return result
     */
    private boolean doCheck(String beanName) throws Exception {
        return checker.check(testBean, beanName, beanFactory);
    }

    @ParameterizedTest
    @MethodSource("provideBeanNameTestCases")
    public void testCheck(String beanName, boolean expected) throws Exception {
        boolean result = doCheck(beanName);
        Assertions.assertEquals(expected, result, "Bean name '" + beanName + "' should return " + expected);
    }

    @Test
    public void testCheckWithNullBeanName() throws Exception {
        boolean result = doCheck(null);
        Assertions.assertTrue(result, "Should return true when bean name is null");
    }

    @Test
    public void testRealConfigBeans() throws Exception {
        // Test common configuration class naming patterns
        Assertions.assertFalse(doCheck("dataSourceConfiguration"));
        Assertions.assertFalse(doCheck("applicationProperties"));
        Assertions.assertFalse(doCheck("serverConfig"));
    }
}
