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
package org.apache.seata.spring.tcc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TccAnnotationProcessorTest {
    private ListAppender<ILoggingEvent> listAppender;
    private TccAnnotationProcessor processor;
    private Set<String> proxied;
    private List<Class<? extends Annotation>> annotations;

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        processor = new TccAnnotationProcessor();

        Logger logger = (Logger) LoggerFactory.getLogger(TccAnnotationProcessor.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Field proxiedField = TccAnnotationProcessor.class.getDeclaredField("PROXIED_SET");
        proxiedField.setAccessible(true);
        proxied = (Set<String>) proxiedField.get(null);

        Field annotationsField = TccAnnotationProcessor.class.getDeclaredField("ANNOTATIONS");
        annotationsField.setAccessible(true);
        annotations = (List<Class<? extends Annotation>>) annotationsField.get(null);
    }

    @AfterEach
    public void tearDown() {
        listAppender.stop();
        listAppender.list.clear();
        proxied.clear();
        annotations.clear();
    }

    @interface MockReference {
    }

    static class MockTccService {
        @TwoPhaseBusinessAction(name = "testAction")
        public void tryMethod() {
        }
    }

    static class TestBean {
        @MockReference
        public MockTccService tccService = new MockTccService();

        @MockReference
        public MockTccService nullService = null;
    }

    @Test
    public void testAddTccAdviseCreatesProxy() throws Exception {
        TestBean bean = new TestBean();
        Field field = TestBean.class.getField("tccService");

        Object originalValue = field.get(bean);

        try (MockedStatic<ProxyUtil> mockedStatic = Mockito.mockStatic(ProxyUtil.class)) {
            mockedStatic.when(() -> ProxyUtil.createProxy(bean, "testBean")).thenAnswer(invocation -> {
                Object arg = invocation.getArgument(0);
                if (arg instanceof TestBean) {
                    return Mockito.spy(new MockTccService());
                }
                return arg;
            });

            processor.addTccAdvise(bean, "testBean", field, MockTccService.class);
        }

        Object newValue = field.get(bean);
        assertNotEquals(originalValue, newValue, "Proxy should replace original field");
        String expectedLog = String.format("Bean[%s] with name [%s] would use proxy", bean.getClass().getName(), "tccService");
        boolean containsLog = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().equals(expectedLog));
        assertTrue(containsLog, "Logs should contain exact proxy injection info: " + expectedLog);
    }

    @Test
    public void testAddTccAdviseFieldValueNull() throws Exception {
        TestBean bean = new TestBean();
        Field nullField = TestBean.class.getField("nullService");
        processor.addTccAdvise(bean, "testBean", nullField, MockTccService.class);
        assertNull(nullField.get(bean));
        boolean noLogsPrinted = listAppender.list.isEmpty();
        assertTrue(noLogsPrinted, "No logs should be printed when field value is null");
    }

    @Test
    public void testProcessWithNullAnnotation() {
        processor.process(new TestBean(), "testBean", null);
        assertTrue(proxied.isEmpty(), "Should not proxy if annotation is null");
    }

    @Test
    public void testProcessWhenAlreadyProxied() {
        proxied.add("testBean");
        processor.process(new TestBean(), "testBean", MockReference.class);
        assertTrue(proxied.contains("testBean"));
    }

    @Test
    public void testProcessFieldWithAnnotation() {
        annotations.add(MockReference.class);
        TestBean bean = new TestBean();

        try (MockedStatic<ProxyUtil> mockedStatic = Mockito.mockStatic(ProxyUtil.class)) {
            mockedStatic.when(() -> ProxyUtil.createProxy(bean, "testBean")).thenReturn(Mockito.spy(new MockTccService()));

            processor.postProcessBeforeInitialization(bean, "testBean");
        }

        assertTrue(proxied.contains("testBean"));
    }

    @Test
    public void testLoadAnnotationWithReflection() throws Exception {
        Method loadAnnotationMethod = TccAnnotationProcessor.class.getDeclaredMethod("loadAnnotation", String.class);
        loadAnnotationMethod.setAccessible(true);

        Class<?> annotationClass = (Class<?>) loadAnnotationMethod.invoke(null, "java.lang.Override");
        assertNotNull(annotationClass);

        Object nullClass = loadAnnotationMethod.invoke(null, "non.existing.AnnotationClass");
        assertNull(nullClass);
    }


    @Test
    public void testPostProcessAfterInitializationReturnsBean() {
        TestBean bean = new TestBean();
        Object result = processor.postProcessAfterInitialization(bean, "testBean");
        assertSame(bean, result);
    }
}
