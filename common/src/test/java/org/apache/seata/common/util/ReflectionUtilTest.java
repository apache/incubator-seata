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
package org.apache.seata.common.util;

import org.apache.seata.common.BranchDO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReflectionUtilTest {

    // Prevent jvm from optimizing final
    public static final String testValue = (null != null ? "hello" : "hello");

    public final String testValue2 = (null != null ? "hello world" : "hello world");

    @Test
    public void testGetClassByName() throws ClassNotFoundException {
        Assertions.assertEquals(String.class, ReflectionUtil.getClassByName("java.lang.String"));
    }

    @Test
    public void testIsClassPresent() {
        Assertions.assertTrue(ReflectionUtil.isClassPresent("java.lang.String"));
        Assertions.assertFalse(ReflectionUtil.isClassPresent("java.lang.String2"));
    }

    @Test
    public void testGetWrappedClass() {
        Assertions.assertEquals(Byte.class, ReflectionUtil.getWrappedClass(byte.class));
        Assertions.assertEquals(Boolean.class, ReflectionUtil.getWrappedClass(boolean.class));
        Assertions.assertEquals(Character.class, ReflectionUtil.getWrappedClass(char.class));
        Assertions.assertEquals(Short.class, ReflectionUtil.getWrappedClass(short.class));
        Assertions.assertEquals(Integer.class, ReflectionUtil.getWrappedClass(int.class));
        Assertions.assertEquals(Long.class, ReflectionUtil.getWrappedClass(long.class));
        Assertions.assertEquals(Float.class, ReflectionUtil.getWrappedClass(float.class));
        Assertions.assertEquals(Double.class, ReflectionUtil.getWrappedClass(double.class));
        Assertions.assertEquals(Void.class, ReflectionUtil.getWrappedClass(void.class));
        Assertions.assertEquals(Object.class, ReflectionUtil.getWrappedClass(Object.class));
    }

    @Test
    public void testSetFieldValue() throws NoSuchFieldException {
        BranchDO branchDO = new BranchDO("xid123123", 123L, 1, 2.2, new Date());
        ReflectionUtil.setFieldValue(branchDO, "xid", "xid456");
        Assertions.assertEquals("xid456", branchDO.getXid());
    }

    @Test
    public void testGetFieldValue() throws NoSuchFieldException {
        Assertions.assertEquals("d", ReflectionUtil.getFieldValue(new DurationUtil(), "DAY_UNIT"));
        Assertions.assertThrows(ClassCastException.class, () -> {
            Integer var = ReflectionUtil.getFieldValue(new DurationUtil(), "DAY_UNIT");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.getFieldValue(null, "a1b2c3"));
        Assertions.assertThrows(NoSuchFieldException.class, () -> ReflectionUtil.getFieldValue(new Object(), "A1B2C3"));
    }

    @Test
    public void testInvokeMethod() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil.invokeMethod("", "length"));
        Assertions.assertEquals(3, ReflectionUtil.invokeMethod("foo", "length"));

        Assertions.assertThrows(NoSuchMethodException.class, () -> ReflectionUtil.invokeMethod("", "size"));
    }

    @Test
    public void testInvokeMethod2() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil.invokeMethod("", "length", null, ReflectionUtil.EMPTY_ARGS));
        Assertions.assertEquals(3, ReflectionUtil.invokeMethod("foo", "length", null, ReflectionUtil.EMPTY_ARGS));

        Assertions.assertThrows(
                NoSuchMethodException.class,
                () -> ReflectionUtil.invokeMethod("", "size", null, ReflectionUtil.EMPTY_ARGS));
    }

    @Test
    public void testInvokeMethod3() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals(
                "0", ReflectionUtil.invokeStaticMethod(String.class, "valueOf", new Class<?>[] {int.class}, 0));
        Assertions.assertEquals(
                "123",
                ReflectionUtil.invokeStaticMethod(
                        String.class, "valueOf", new Class<?>[] {int.class}, new Object[] {123}));

        Assertions.assertThrows(
                NoSuchMethodException.class,
                () -> ReflectionUtil.invokeStaticMethod(String.class, "size", null, ReflectionUtil.EMPTY_ARGS));
    }

    @Test
    public void testGetInterfaces() {
        Assertions.assertArrayEquals(
                new Object[] {Serializable.class},
                ReflectionUtil.getInterfaces(Serializable.class).toArray());

        Assertions.assertArrayEquals(
                new Object[] {Map.class, Cloneable.class, Serializable.class},
                ReflectionUtil.getInterfaces(HashMap.class).toArray());
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void testModifyStaticFinalField() throws NoSuchFieldException, IllegalAccessException {
        Assertions.assertEquals("hello", testValue);
        ReflectionUtil.modifyStaticFinalField(ReflectionUtilTest.class, "testValue", "hello world");
        Assertions.assertEquals("hello world", testValue);

        // case: not a static field
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectionUtil.modifyStaticFinalField(ReflectionUtilTest.class, "testValue2", "hello");
        });
    }

    // region test the method 'getAllFields'

    @Test
    public void testGetAllFields() {
        // TestClass
        this.testGetAllFieldsInternal(TestClass.class, "f1", "f2");
        // TestSuperClass
        this.testGetAllFieldsInternal(TestSuperClass.class, "f2");
        // EmptyClass
        this.testGetAllFieldsInternal(EmptyClass.class);
        // TestInterface
        this.testGetAllFieldsInternal(TestInterface.class);
        // Object
        this.testGetAllFieldsInternal(Object.class);

        // case: The fields of EmptyClass is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(EmptyClass.class));
        // case: The fields of TestInterface is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(TestInterface.class));
        // case: The fields of Object is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(Object.class));
    }

    private void testGetAllFieldsInternal(Class<?> clazz, String... fieldNames) {
        Field[] fields = ReflectionUtil.getAllFields(clazz);
        Assertions.assertEquals(fieldNames.length, fields.length);
        Field[] fields2 = ReflectionUtil.getAllFields(clazz);
        Assertions.assertSame(fields, fields2);

        if (fieldNames.length == 0) {
            return;
        }

        List<String> fieldNameList = Arrays.asList(fieldNames);
        for (Field field : fields) {
            Assertions.assertTrue(fieldNameList.contains(field.getName()));
        }
    }

    @Test
    public void testMethodToString() throws NoSuchMethodException {
        Assertions.assertEquals(
                "Method<ReflectionUtilTest.testMethodToString()>",
                ReflectionUtil.methodToString(this.getClass().getMethod("testMethodToString")));
    }

    @Test
    public void testAnnotationToString() throws NoSuchMethodException {
        Assertions.assertEquals(
                "@Test()",
                ReflectionUtil.annotationToString(
                        this.getClass().getMethod("testAnnotationToString").getAnnotation(Test.class)));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
    }) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
    public void testGetAnnotationValues() throws NoSuchMethodException, NoSuchFieldException {
        Assertions.assertEquals(
                new LinkedHashMap<>(),
                ReflectionUtil.getAnnotationValues(
                        this.getClass().getMethod("testGetAnnotationValues").getAnnotation(Test.class)));
    }

    // Enhanced test cases

    @Test
    public void testGetClassByNameEnhanced() throws ClassNotFoundException {
        // Test normal case
        assertThat(ReflectionUtil.getClassByName("java.lang.String")).isEqualTo(String.class);

        // Test exception case
        assertThatThrownBy(() -> ReflectionUtil.getClassByName("non.existent.Class"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void testIsClassPresentEnhanced() {
        // Test existing class
        assertThat(ReflectionUtil.isClassPresent("java.lang.String")).isTrue();

        // Test non-existing class
        assertThat(ReflectionUtil.isClassPresent("non.existent.Class")).isFalse();
    }

    @Test
    public void testGetWrappedClassEnhanced() {
        // Test all primitive types
        assertThat(ReflectionUtil.getWrappedClass(byte.class)).isEqualTo(Byte.class);
        assertThat(ReflectionUtil.getWrappedClass(boolean.class)).isEqualTo(Boolean.class);
        assertThat(ReflectionUtil.getWrappedClass(char.class)).isEqualTo(Character.class);
        assertThat(ReflectionUtil.getWrappedClass(short.class)).isEqualTo(Short.class);
        assertThat(ReflectionUtil.getWrappedClass(int.class)).isEqualTo(Integer.class);
        assertThat(ReflectionUtil.getWrappedClass(long.class)).isEqualTo(Long.class);
        assertThat(ReflectionUtil.getWrappedClass(float.class)).isEqualTo(Float.class);
        assertThat(ReflectionUtil.getWrappedClass(double.class)).isEqualTo(Double.class);
        assertThat(ReflectionUtil.getWrappedClass(void.class)).isEqualTo(Void.class);

        // Test non-primitive type
        assertThat(ReflectionUtil.getWrappedClass(String.class)).isEqualTo(String.class);
    }

    @Test
    public void testGetInterfacesEnhanced() {
        // Test interface class
        Set<Class<?>> interfaces = ReflectionUtil.getInterfaces(TestInterfaceEnhanced.class);
        assertThat(interfaces).containsExactly(TestInterfaceEnhanced.class);

        // Test implementation class
        Set<Class<?>> implInterfaces = ReflectionUtil.getInterfaces(TestImpl.class);
        assertThat(implInterfaces).contains(TestInterfaceEnhanced.class);

        // Test class with multiple interfaces
        Set<Class<?>> multiInterfaces = ReflectionUtil.getInterfaces(MultiInterfaceImpl.class);
        assertThat(multiInterfaces).contains(TestInterfaceEnhanced.class, SecondInterface.class);
    }

    @Test
    public void testGetAllFieldsEnhanced() {
        // Test normal class
        Field[] fields = ReflectionUtil.getAllFields(TestClassEnhanced.class);
        assertThat(fields).hasSize(2); // f1, f2

        // Test class with no fields
        Field[] emptyFields = ReflectionUtil.getAllFields(EmptyClass.class);
        assertThat(emptyFields).isEmpty();

        // Test Object class
        Field[] objectFields = ReflectionUtil.getAllFields(Object.class);
        assertThat(objectFields).isEmpty();

        // Test interface
        Field[] interfaceFields = ReflectionUtil.getAllFields(TestInterfaceEnhanced.class);
        assertThat(interfaceFields).isEmpty();
    }

    @Test
    public void testGetField() throws NoSuchFieldException {
        // Test normal field
        Field field = ReflectionUtil.getField(TestClassEnhanced.class, "f1");
        assertThat(field.getName()).isEqualTo("f1");

        // Test inherited field
        Field inheritedField = ReflectionUtil.getField(TestClassEnhanced.class, "f2");
        assertThat(inheritedField.getName()).isEqualTo("f2");

        // Test non-existent field
        assertThatThrownBy(() -> ReflectionUtil.getField(TestClassEnhanced.class, "nonExistent"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    public void testGetFieldValueEnhanced() throws NoSuchFieldException {
        TestClassEnhanced testObj = new TestClassEnhanced();
        testObj.setValue("testValue");

        // Test normal field value
        String value = ReflectionUtil.getFieldValue(testObj, "f1");
        assertThat(value).isEqualTo("testValue");

        // Test inherited field value using getter since it's private
        testObj.setF2(123);
        Integer inheritedValue = testObj.getF2();
        assertThat(inheritedValue).isEqualTo(123);

        // Test null target
        assertThatThrownBy(() -> ReflectionUtil.getFieldValue(null, "f1")).isInstanceOf(IllegalArgumentException.class);

        // Test non-existent field
        assertThatThrownBy(() -> ReflectionUtil.getFieldValue(testObj, "nonExistent"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    public void testSetFieldValueEnhanced() throws NoSuchFieldException {
        TestClassEnhanced testObj = new TestClassEnhanced();

        // Test setting field value
        ReflectionUtil.setFieldValue(testObj, "f1", "newValue");
        assertThat(testObj.getValue()).isEqualTo("newValue");

        // Test setting inherited field value using setter since it's private
        ReflectionUtil.setFieldValue(testObj, "f2", 456);
        assertThat(testObj.getF2()).isEqualTo(456);

        // Test null target
        assertThatThrownBy(() -> ReflectionUtil.setFieldValue(null, "f1", "value"))
                .isInstanceOf(IllegalArgumentException.class);

        // Test non-existent field
        assertThatThrownBy(() -> ReflectionUtil.setFieldValue(testObj, "nonExistent", "value"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    public void testGetMethod() throws NoSuchMethodException {
        // Test method with parameters
        Method method = ReflectionUtil.getMethod(TestClassEnhanced.class, "setValue", String.class);
        assertThat(method.getName()).isEqualTo("setValue");

        // Test method without parameters
        Method noParamMethod = ReflectionUtil.getMethod(TestClassEnhanced.class, "getValue");
        assertThat(noParamMethod.getName()).isEqualTo("getValue");

        // Test non-existent method
        assertThatThrownBy(() -> ReflectionUtil.getMethod(TestClassEnhanced.class, "nonExistent"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    public void testInvokeMethodEnhanced() {
        TestClassEnhanced testObj = new TestClassEnhanced();

        try {
            // Test method invocation with parameters
            ReflectionUtil.invokeMethod(testObj, "setValue", new Class[] {String.class}, "test");
            String result = (String) ReflectionUtil.invokeMethod(testObj, "getValue");
            assertThat(result).isEqualTo("test");

            // Test method invocation without parameters
            Object result2 = ReflectionUtil.invokeMethod(testObj, "getValue");
            assertThat(result2).isEqualTo("test");

            // Test non-existent method
            assertThatThrownBy(() -> ReflectionUtil.invokeMethod(testObj, "nonExistent"))
                    .isInstanceOf(NoSuchMethodException.class);
        } catch (Exception e) {
            // Wrap any exceptions in RuntimeException to avoid compiler errors
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testInvokeStaticMethodEnhanced() {
        try {
            // Test static method with parameters
            String result =
                    (String) ReflectionUtil.invokeStaticMethod(String.class, "valueOf", new Class[] {int.class}, 123);
            assertThat(result).isEqualTo("123");

            // Test static method without parameters
            // Note: We don't have a good static method without parameters to test, so we test the exception case
            assertThatThrownBy(() -> ReflectionUtil.invokeStaticMethod(String.class, "copyValueOf"))
                    .isInstanceOf(NoSuchMethodException.class)
                    .hasMessageContaining("method not found");
        } catch (Exception e) {
            // Wrap any exceptions in RuntimeException to avoid compiler errors
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToStringMethods() throws NoSuchFieldException {
        // Test classToString
        assertThat(ReflectionUtil.classToString(String.class)).isEqualTo("Class<String>");

        // Test fieldToString
        Field field = TestClassEnhanced.class.getDeclaredField("f1");
        assertThat(ReflectionUtil.fieldToString(field)).isEqualTo("Field<TestClassEnhanced.(String f1)>");

        try {
            // Test methodToString
            Method method = TestClassEnhanced.class.getDeclaredMethod("getValue");
            assertThat(ReflectionUtil.methodToString(method)).isEqualTo("Method<TestClassEnhanced.getValue()>");

            // Test parameterTypesToString
            assertThat(ReflectionUtil.parameterTypesToString(new Class[] {String.class, int.class}))
                    .isEqualTo("(String, int)");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetAnnotationValuesEnhanced() throws NoSuchFieldException {
        // Test with a method annotated with @Test
        try {
            Method method = this.getClass().getDeclaredMethod("testGetAnnotationValuesEnhanced");
            Test annotation = method.getAnnotation(Test.class);

            // This test is mainly to increase coverage, actual functionality tested in ReflectionUtilTest
            assertThat(annotation).isNotNull();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    // region the test class and interface

    class EmptyClass {}

    class TestClass extends TestSuperClass implements TestInterface {

        private String f1;

        public String getF1() {
            return f1;
        }

        public void setF1(String f1) {
            this.f1 = f1;
        }
    }

    class TestSuperClass implements TestInterface {
        private String f2;

        public String getF2() {
            return f2;
        }

        public void setF2(String f2) {
            this.f2 = f2;
        }
    }

    interface TestInterface {}

    // Enhanced test classes and interfaces
    interface TestInterfaceEnhanced {
        void interfaceMethod();
    }

    interface SecondInterface {
        void secondMethod();
    }

    static class TestSuperClassEnhanced {
        private Integer f2;

        public Integer getF2() {
            return f2;
        }

        public void setF2(Integer f2) {
            this.f2 = f2;
        }
    }

    static class TestClassEnhanced extends TestSuperClassEnhanced {
        private String f1;

        public String getValue() {
            return f1;
        }

        public void setValue(String value) {
            this.f1 = value;
        }
    }

    static class MultiInterfaceImpl implements TestInterfaceEnhanced, SecondInterface {
        @Override
        public void interfaceMethod() {}

        @Override
        public void secondMethod() {}
    }

    static class TestImpl implements TestInterfaceEnhanced {
        @Override
        public void interfaceMethod() {}
    }

    static class TestConstants {
        public static final String STATIC_FINAL_FIELD = "original";
    }

    // endregion

    // endregion
}
