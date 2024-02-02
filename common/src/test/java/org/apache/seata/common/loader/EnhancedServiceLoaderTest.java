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
package org.apache.seata.common.loader;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.util.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Enhanced service loader test.
 *
 */
public class EnhancedServiceLoaderTest {

    /**
     * Test load by class and class loader.
     */
    @Test
    public void testLoadByClassAndClassLoader() {
        Hello load = EnhancedServiceLoader.load(Hello.class, Hello.class.getClassLoader());
        Assertions.assertEquals(load.say(), "Olá.");
    }

    /**
     * Test load exception.
     */
    @Test
    public void testLoadException() {
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            EnhancedServiceLoaderTest load = EnhancedServiceLoader.load(EnhancedServiceLoaderTest.class);
        });
    }

    /**
     * Test load by class.
     */
    @Test
    public void testLoadByClass() {
        Hello load = EnhancedServiceLoader.load(Hello.class);
        assertThat(load.say()).isEqualTo("Olá.");
    }

    /**
     * Test load by class and activate name.
     */
    @Test
    public void testLoadByClassAndActivateName() {
        Hello englishHello = EnhancedServiceLoader.load(Hello.class, "EnglishHello");
        assertThat(englishHello.say()).isEqualTo("hello!");
    }

    /**
     * Test load by class and class loader and activate name.
     */
    @Test
    public void testLoadByClassAndClassLoaderAndActivateName() {
        Hello englishHello = EnhancedServiceLoader
                .load(Hello.class, "EnglishHello", EnhancedServiceLoaderTest.class.getClassLoader());
        assertThat(englishHello.say()).isEqualTo("hello!");
    }

    /**
     * Gets all extension class.
     */
    @Test
    public void getAllExtensionClass() {
        List<Class<Hello>> allExtensionClass = EnhancedServiceLoader.getAllExtensionClass(Hello.class);
        assertThat(allExtensionClass.get(3).getSimpleName()).isEqualTo((LatinHello.class.getSimpleName()));
        assertThat(allExtensionClass.get(2).getSimpleName()).isEqualTo((FrenchHello.class.getSimpleName()));
        assertThat(allExtensionClass.get(1).getSimpleName()).isEqualTo((EnglishHello.class.getSimpleName()));
        assertThat(allExtensionClass.get(0).getSimpleName()).isEqualTo((ChineseHello.class.getSimpleName()));
    }

    /**
     * Gets all extension class 1.
     */
    @Test
    public void getAllExtensionClass1() {
        List<Class<Hello>> allExtensionClass = EnhancedServiceLoader
                .getAllExtensionClass(Hello.class, ClassLoader.getSystemClassLoader());
        assertThat(allExtensionClass).isNotEmpty();
    }

    @Test
    public void getSingletonExtensionInstance(){
        Hello hello1 = EnhancedServiceLoader.load(Hello.class, "ChineseHello");
        Hello hello2 = EnhancedServiceLoader.load(Hello.class, "ChineseHello");
        assertThat(hello1 == hello2).isTrue();
    }

    @Test
    public void getMultipleExtensionInstance(){
        Hello hello1 = EnhancedServiceLoader.load(Hello.class, "LatinHello");
        Hello hello2 = EnhancedServiceLoader.load(Hello.class, "LatinHello");
        assertThat(hello1 == hello2).isFalse();
    }

    @Test
    public void getAllInstances(){
        List<Hello> hellows1 = EnhancedServiceLoader.loadAll(Hello.class);
        List<Hello> hellows2 = EnhancedServiceLoader.loadAll(Hello.class);
        for (Hello hello : hellows1){
            if (!hello.say().equals("Olá.")) {
                assertThat(hellows2.contains(hello)).isTrue();
            }
            else{
                assertThat(hellows2.contains(hello)).isFalse();
            }
        }
    }

    @Test
    public void classCastExceptionTest() {
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            Hello1 load = EnhancedServiceLoader.load(Hello1.class);
        });
    }

    @Test
    public void testLoadByClassAndActivateNameAndArgs() {
        Hello2 load = EnhancedServiceLoader.load(Hello2.class, "JapaneseHello", new Object[] {"msg"});
        assertThat(load).isInstanceOf(Hello2.class);
    }

    @Test
    public void testLoadByClassAndActivateNameAndArgsTypeAndArgs() {
        Hello2 load = EnhancedServiceLoader
                .load(Hello2.class, "JapaneseHello", new Class[] {String.class}, new Object[] {"msg"});
        assertThat(load).isInstanceOf(Hello2.class);
    }

    @Test
    public void testUnloadAll() throws NoSuchFieldException, IllegalAccessException {
        Hello hello = EnhancedServiceLoader.load(Hello.class);
        assertThat(hello).isInstanceOf(Hello.class);
        Hello2 hello2 = EnhancedServiceLoader.load(Hello2.class, "JapaneseHello", new Object[]{"msg"});
        assertThat(hello2).isInstanceOf(Hello2.class);

        EnhancedServiceLoader.unloadAll();

        Class<EnhancedServiceLoader> clazz = EnhancedServiceLoader.class;
        Field serviceLoadersField = clazz.getDeclaredField("SERVICE_LOADERS");
        serviceLoadersField.setAccessible(true);
        Map<Class<?>, Object> serviceLoaders = (Map<Class<?>, Object>)serviceLoadersField.get(null);
        assertThat(CollectionUtils.isEmpty(serviceLoaders)).isTrue();
    }

    @Test
    public void testUnloadByClass() throws NoSuchFieldException, IllegalAccessException {
        Hello load = EnhancedServiceLoader.load(Hello.class);
        assertThat(load).isInstanceOf(Hello.class);

        EnhancedServiceLoader.unload(Hello.class);
        // get serviceLoaders
        Class<EnhancedServiceLoader> clazz = EnhancedServiceLoader.class;
        Field serviceLoadersField = clazz.getDeclaredField("SERVICE_LOADERS");
        serviceLoadersField.setAccessible(true);
        Map<Class<?>, Object> serviceLoaders = (Map<Class<?>, Object>)serviceLoadersField.get(null);

        assertThat(serviceLoaders.get(Hello.class)).isNull();
    }

    // FIXME: 2023/2/11 wait fix EnhancedServiceLoader.unload(Class<S> service, String activateName)
    // @Test
    public void testUnloadByClassAndActivateName() throws NoSuchFieldException, IllegalAccessException {
        Hello englishHello = EnhancedServiceLoader.load(Hello.class, "EnglishHello");
        assertThat(englishHello.say()).isEqualTo("hello!");

        EnhancedServiceLoader.unload(Hello.class, "EnglishHello");
        // get serviceLoaders
        Class<EnhancedServiceLoader> clazz = EnhancedServiceLoader.class;
        Field serviceLoadersField = clazz.getDeclaredField("SERVICE_LOADERS");
        serviceLoadersField.setAccessible(true);
        Map<Class<?>, Object> serviceLoaders = (Map<Class<?>, Object>)serviceLoadersField.get(null);
        //get innerEnhancedServiceLoader.classToDefinitionMap
        Object innerEnhancedServiceLoader = serviceLoaders.get(Hello.class);
        Field classToDefinitionMapField = innerEnhancedServiceLoader.getClass().getDeclaredField("classToDefinitionMap");
        classToDefinitionMapField.setAccessible(true);
        Map<Class<?>, Object> classToDefinitionMap = (Map<Class<?>, Object>) classToDefinitionMapField.get(innerEnhancedServiceLoader);

        assertThat(classToDefinitionMap.get(EnglishHello.class)).isNull();
    }

    @Test
    public void testUnload() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> EnhancedServiceLoader.unload(Hello.class, null));
        Hello load = EnhancedServiceLoader.load(Hello.class, "FrenchHello");
        Assertions.assertDoesNotThrow(() -> EnhancedServiceLoader.unload(Hello.class, "FrenchHello"));
    }


}
