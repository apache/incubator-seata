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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Scope;

public class ScopeBeansScannerCheckerTest {

    @Scope(scopeName = "request")
    static class RequestBean {
    }

    static class NormalBean {
    }

    @Scope(scopeName = "session")
    static class SessionBean {
    }

    @Scope(scopeName = "job")
    static class JobBean {
    }

    @Scope(scopeName = "step")
    static class StepBean {
    }

    @Scope(scopeName = "singleton")
    static class SingletonBean {
    }

    @Scope(scopeName = "prototype")
    static class PrototypeBean {
    }

    @Scope(scopeName = "")
    static class EmptyScopeBean {
    }

    @Test
    public void testCheck_nullFactoryPasses() throws Exception {
        ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();
        Assertions.assertTrue(checker.check(new NormalBean(), "normalBean", null));
    }

    @Test
    public void testCheck_requestScopeExcluded() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(factory);
        reader.register(RequestBean.class);
        String beanName = factory.getBeanNamesForType(RequestBean.class)[0];
        ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();
        boolean result = checker.check(new RequestBean(), beanName, factory);
        Assertions.assertFalse(result);
    }

    @Test
    public void testCheck_sessionJobStepExcluded() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(factory);
        reader.register(SessionBean.class, JobBean.class, StepBean.class);
        ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();

        String sessionName = factory.getBeanNamesForType(SessionBean.class)[0];
        String jobName = factory.getBeanNamesForType(JobBean.class)[0];
        String stepName = factory.getBeanNamesForType(StepBean.class)[0];
        Assertions.assertFalse(checker.check(new SessionBean(), sessionName, factory));
        Assertions.assertFalse(checker.check(new JobBean(), jobName, factory));
        Assertions.assertFalse(checker.check(new StepBean(), stepName, factory));
    }

    @Test
    public void testCheck_singletonPrototypePass() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(factory);
        reader.register(SingletonBean.class, PrototypeBean.class);
        ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();

        Assertions.assertTrue(checker.check(new SingletonBean(), "singletonBean", factory));
        Assertions.assertTrue(checker.check(new PrototypeBean(), "prototypeBean", factory));
    }

    @Test
    public void testCheck_scopeAnnotationPresentButEmptyValue() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(factory);
        reader.register(EmptyScopeBean.class);
        ScopeBeansScannerChecker checker = new ScopeBeansScannerChecker();
        // Empty scope string should not be excluded
        Assertions.assertTrue(checker.check(new EmptyScopeBean(), "emptyScopeBean", factory));
    }
}
