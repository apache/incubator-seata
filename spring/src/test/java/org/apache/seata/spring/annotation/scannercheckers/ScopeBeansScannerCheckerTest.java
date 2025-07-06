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
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Test for {@link ScopeBeansScannerChecker} class
 */
public class ScopeBeansScannerCheckerTest {

    private final ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();
    private final Object testBean = new Object();
    private Set<String> originalExcludeScopes;

    /**
     * Setup before each test - save original exclude scopes
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Save original exclude scopes
        originalExcludeScopes = getExcludeScopeSet();
    }

    /**
     * Cleanup after each test - restore original exclude scopes
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Restore original exclude scopes
        setExcludeScopeSet(originalExcludeScopes);
    }

    /**
     * Test check method when beanFactory is null
     */
    @Test
    public void testCheckWhenBeanFactoryIsNull() throws Exception {
        boolean result = checker.check(testBean, "testBean", null);
        Assertions.assertTrue(result, "Should return true when beanFactory is null");
    }

    /**
     * Test check method when bean definition is not found
     */
    @Test
    public void testCheckWhenBeanDefinitionNotFound() throws Exception {
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString()))
                .thenThrow(new NoSuchBeanDefinitionException("testBean"));

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when bean definition is not found");
    }

    /**
     * Test check method when bean definition is not an AnnotatedBeanDefinition
     */
    @Test
    public void testCheckWhenBeanDefinitionIsNotAnnotated() throws Exception {
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        BeanDefinition beanDefinition = Mockito.mock(BeanDefinition.class);

        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString())).thenReturn(beanDefinition);
        Mockito.when(beanDefinition.getOriginatingBeanDefinition()).thenReturn(null);

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when bean definition is not an AnnotatedBeanDefinition");
    }

    /**
     * Test check method with non-excluded scope
     */
    @Test
    public void testCheckWithNonExcludedScope() throws Exception {
        // Mock objects
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        AnnotatedBeanDefinition beanDefinition = Mockito.mock(AnnotatedBeanDefinition.class);
        AnnotationMetadata metadata = Mockito.mock(AnnotationMetadata.class);

        // Setup scope attributes with a non-excluded scope
        MultiValueMap<String, Object> scopeAttributes = new LinkedMultiValueMap<>();
        scopeAttributes.add("scopeName", "singleton");

        // Setup mock behavior
        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString())).thenReturn(beanDefinition);
        Mockito.when(beanDefinition.getMetadata()).thenReturn(metadata);
        Mockito.when(beanDefinition.getFactoryMethodMetadata()).thenReturn(null);
        Mockito.when(metadata.getAllAnnotationAttributes(Mockito.eq(Scope.class.getName())))
                .thenReturn(scopeAttributes);

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when scope is not excluded");
    }

    /**
     * Test check method with excluded scope
     */
    @Test
    public void testCheckWithExcludedScope() throws Exception {
        // Mock objects
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        AnnotatedBeanDefinition beanDefinition = Mockito.mock(AnnotatedBeanDefinition.class);
        AnnotationMetadata metadata = Mockito.mock(AnnotationMetadata.class);

        // Setup scope attributes with an excluded scope (request)
        MultiValueMap<String, Object> scopeAttributes = new LinkedMultiValueMap<>();
        scopeAttributes.add("scopeName", ScopeBeansScannerChecker.REQUEST_SCOPE_NAME);

        // Setup mock behavior
        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString())).thenReturn(beanDefinition);
        Mockito.when(beanDefinition.getMetadata()).thenReturn(metadata);
        Mockito.when(beanDefinition.getFactoryMethodMetadata()).thenReturn(null);
        Mockito.when(metadata.getAllAnnotationAttributes(Mockito.eq(Scope.class.getName())))
                .thenReturn(scopeAttributes);

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertFalse(result, "Should return false when scope is excluded");
    }

    /**
     * Test check method with factory method metadata having excluded scope
     */
    @Test
    public void testCheckWithFactoryMethodHavingExcludedScope() throws Exception {
        // Mock objects
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        AnnotatedBeanDefinition beanDefinition = Mockito.mock(AnnotatedBeanDefinition.class);
        AnnotationMetadata metadata = Mockito.mock(AnnotationMetadata.class);
        MethodMetadata factoryMethodMetadata = Mockito.mock(MethodMetadata.class);

        // Setup factory method metadata with an excluded scope (session)
        MultiValueMap<String, Object> factoryScopeAttributes = new LinkedMultiValueMap<>();
        factoryScopeAttributes.add("scopeName", ScopeBeansScannerChecker.SESSION_SCOPE_NAME);

        // Setup mock behavior
        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString())).thenReturn(beanDefinition);
        Mockito.when(beanDefinition.getMetadata()).thenReturn(metadata);
        Mockito.when(beanDefinition.getFactoryMethodMetadata()).thenReturn(factoryMethodMetadata);
        Mockito.when(factoryMethodMetadata.getAllAnnotationAttributes(Mockito.eq(Scope.class.getName())))
                .thenReturn(factoryScopeAttributes);
        Mockito.when(metadata.getAllAnnotationAttributes(Mockito.eq(Scope.class.getName())))
                .thenReturn(null);

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertFalse(result, "Should return false when factory method has excluded scope");
    }

    /**
     * Test check method with no scope annotation
     */
    @Test
    public void testCheckWithNoScopeAnnotation() throws Exception {
        // Mock objects
        ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        AnnotatedBeanDefinition beanDefinition = Mockito.mock(AnnotatedBeanDefinition.class);
        AnnotationMetadata metadata = Mockito.mock(AnnotationMetadata.class);

        // Setup mock behavior
        Mockito.when(beanFactory.getBeanDefinition(Mockito.anyString())).thenReturn(beanDefinition);
        Mockito.when(beanDefinition.getMetadata()).thenReturn(metadata);
        Mockito.when(beanDefinition.getFactoryMethodMetadata()).thenReturn(null);
        Mockito.when(metadata.getAllAnnotationAttributes(Mockito.eq(Scope.class.getName())))
                .thenReturn(null);

        boolean result = checker.check(testBean, "testBean", beanFactory);
        Assertions.assertTrue(result, "Should return true when no scope annotation is present");
    }

    /**
     * Test addExcludeScopes method
     */
    @Test
    public void testAddExcludeScopes() throws Exception {
        // Clear exclude scopes first
        Set<String> originalSet = getExcludeScopeSet();
        setExcludeScopeSet(new HashSet<>());

        // Add exclude scopes
        ScopeBeansScannerChecker.addExcludeScopes("custom1", "custom2");

        // Verify scopes were added
        Set<String> excludeScopes = getExcludeScopeSet();
        Assertions.assertEquals(2, excludeScopes.size(), "Should have 2 exclude scopes");
        Assertions.assertTrue(excludeScopes.contains("custom1"), "Should contain 'custom1'");
        Assertions.assertTrue(excludeScopes.contains("custom2"), "Should contain 'custom2'");

        // Restore original set
        setExcludeScopeSet(originalSet);
    }

    /**
     * Test addExcludeScopes with blank scopes
     */
    @Test
    public void testAddExcludeScopesWithBlankScopes() throws Exception {
        // Clear exclude scopes first
        Set<String> originalSet = getExcludeScopeSet();
        setExcludeScopeSet(new HashSet<>());

        // Add exclude scopes including blank ones
        ScopeBeansScannerChecker.addExcludeScopes("custom", "", "  ", null);

        // Verify only non-blank scopes were added
        Set<String> excludeScopes = getExcludeScopeSet();
        Assertions.assertEquals(1, excludeScopes.size(), "Should have 1 exclude scope");
        Assertions.assertTrue(excludeScopes.contains("custom"), "Should contain 'custom'");

        // Restore original set
        setExcludeScopeSet(originalSet);
    }

    /**
     * Test default exclude scopes
     */
    @Test
    public void testDefaultExcludeScopes() throws Exception {
        Set<String> excludeScopes = getExcludeScopeSet();
        Assertions.assertTrue(
                excludeScopes.contains(ScopeBeansScannerChecker.REQUEST_SCOPE_NAME),
                "Should contain 'request' scope by default");
        Assertions.assertTrue(
                excludeScopes.contains(ScopeBeansScannerChecker.SESSION_SCOPE_NAME),
                "Should contain 'session' scope by default");
        Assertions.assertTrue(
                excludeScopes.contains(ScopeBeansScannerChecker.JOB_SCOPE_NAME),
                "Should contain 'job' scope by default");
        Assertions.assertTrue(
                excludeScopes.contains(ScopeBeansScannerChecker.STEP_SCOPE_NAME),
                "Should contain 'step' scope by default");
    }

    /**
     * Helper method to get the EXCLUDE_SCOPE_SET field value
     */
    @SuppressWarnings("unchecked")
    private Set<String> getExcludeScopeSet() throws Exception {
        Field field = ScopeBeansScannerChecker.class.getDeclaredField("EXCLUDE_SCOPE_SET");
        field.setAccessible(true);
        Set<String> scopes = (Set<String>) field.get(null);
        return new HashSet<>(scopes); // Return a copy to avoid modifying the original
    }

    /**
     * Helper method to set the EXCLUDE_SCOPE_SET field value
     */
    @SuppressWarnings("unchecked")
    private void setExcludeScopeSet(Set<String> scopes) throws Exception {
        Field field = ScopeBeansScannerChecker.class.getDeclaredField("EXCLUDE_SCOPE_SET");
        field.setAccessible(true);
        Set<String> originalSet = (Set<String>) field.get(null);
        originalSet.clear();
        originalSet.addAll(scopes);
    }
}
