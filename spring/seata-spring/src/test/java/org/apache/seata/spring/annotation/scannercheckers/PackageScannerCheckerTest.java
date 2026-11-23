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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Test for {@link PackageScannerChecker} class
 */
public class PackageScannerCheckerTest {

    private final PackageScannerChecker checker = new PackageScannerChecker();
    private final ConfigurableListableBeanFactory beanFactory = null;
    private Set<String> originalPackages;

    /**
     * Setup before each test - save original packages and clear the set
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Save original packages
        originalPackages = getScannablePackageSet();

        // Clear the package set for testing
        clearScannablePackageSet();
    }

    /**
     * Cleanup after each test - restore original packages
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Restore original packages
        clearScannablePackageSet();
        if (originalPackages != null) {
            for (String pkg : originalPackages) {
                PackageScannerChecker.addScannablePackages(pkg);
            }
        }
    }

    /**
     * Test check method when package set is empty
     */
    @Test
    public void testCheckWhenPackageSetEmpty() throws Exception {
        // When package set is empty, should return true for any bean
        Object testBean = new Object();
        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when package set is empty");
    }

    /**
     * Test check method with matching package
     */
    @Test
    public void testCheckWithMatchingPackage() throws Exception {
        // Add test packages
        PackageScannerChecker.addScannablePackages("org.apache.seata");

        // Test with bean in the scannable package
        TestBean testBean = new TestBean();
        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when bean's package matches scannable package");
    }

    /**
     * Test check method with non-matching package
     */
    @Test
    public void testCheckWithNonMatchingPackage() throws Exception {
        // Add test packages
        PackageScannerChecker.addScannablePackages("com.example");

        // Test with bean not in the scannable package
        TestBean testBean = new TestBean();
        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertFalse(result, "Should return false when bean's package doesn't match scannable package");
    }

    /**
     * Test addScannablePackages method
     */
    @Test
    public void testAddScannablePackages() throws Exception {
        // Add test packages
        PackageScannerChecker.addScannablePackages("com.example", "org.apache.seata");

        // Verify packages were added
        Set<String> packages = getScannablePackageSet();
        Assertions.assertEquals(2, packages.size(), "Should have 2 packages in the set");
        Assertions.assertTrue(packages.contains("com.example"), "Should contain 'com.example'");
        Assertions.assertTrue(packages.contains("org.apache.seata"), "Should contain 'org.apache.seata'");
    }

    /**
     * Test addScannablePackages with blank packages
     */
    @Test
    public void testAddScannablePackagesWithBlankPackages() throws Exception {
        // Add test packages including blank ones
        PackageScannerChecker.addScannablePackages("com.example", "", "  ", null);

        // Verify only non-blank packages were added
        Set<String> packages = getScannablePackageSet();
        Assertions.assertEquals(1, packages.size(), "Should have 1 package in the set");
        Assertions.assertTrue(packages.contains("com.example"), "Should contain 'com.example'");
    }

    /**
     * Test addScannablePackages with null
     */
    @Test
    public void testAddScannablePackagesWithNull() throws Exception {
        // Add null packages
        PackageScannerChecker.addScannablePackages((String[]) null);

        // Verify no packages were added
        Set<String> packages = getScannablePackageSet();
        Assertions.assertTrue(packages.isEmpty(), "Package set should be empty");
    }

    /**
     * Test case sensitivity in package names
     */
    @Test
    public void testPackageNameCaseSensitivity() throws Exception {
        // Add test packages with mixed case
        PackageScannerChecker.addScannablePackages("Com.Example");

        // Verify package was added in lowercase
        Set<String> packages = getScannablePackageSet();
        Assertions.assertEquals(1, packages.size(), "Should have 1 package in the set");
        Assertions.assertTrue(packages.contains("com.example"), "Should contain lowercase 'com.example'");
        Assertions.assertFalse(packages.contains("Com.Example"), "Should not contain 'Com.Example' with original case");
    }

    /**
     * Helper method to get the SCANNABLE_PACKAGE_SET field value
     */
    @SuppressWarnings("unchecked")
    private Set<String> getScannablePackageSet() throws Exception {
        Field field = PackageScannerChecker.class.getDeclaredField("SCANNABLE_PACKAGE_SET");
        field.setAccessible(true);
        Set<String> packages = (Set<String>) field.get(null);
        return new HashSet<>(packages); // Return a copy to avoid modifying the original
    }

    /**
     * Helper method to clear the SCANNABLE_PACKAGE_SET
     */
    private void clearScannablePackageSet() throws Exception {
        Field field = PackageScannerChecker.class.getDeclaredField("SCANNABLE_PACKAGE_SET");
        field.setAccessible(true);
        Set<String> packages = (Set<String>) field.get(null);
        packages.clear();
    }

    /**
     * Test bean class for package testing
     */
    private static class TestBean {
        // Empty class for testing
    }
}
