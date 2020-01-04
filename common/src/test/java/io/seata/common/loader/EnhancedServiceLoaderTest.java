/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Enhanced service loader test.
 *
 * @author Otis.z
 */
public class EnhancedServiceLoaderTest {

    /**
     * Test load by class and class loader.
     */
    @Test
    public void testLoadByClassAndClassLoader() {
        Hello load = EnhancedServiceLoader.getServiceLoader(Hello.class).load(Hello.class.getClassLoader());
        Assertions.assertEquals(load.say(), "Olá.");
    }

    /**
     * Test load exception.
     */
    @Test
    public void testLoadException() {
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            EnhancedServiceLoaderTest load = EnhancedServiceLoader.getServiceLoader(EnhancedServiceLoaderTest.class).load();
        });
    }

    /**
     * Test load by class.
     */
    @Test
    public void testLoadByClass() {
        Hello load = EnhancedServiceLoader.getServiceLoader(Hello.class).load();
        assertThat(load.say()).isEqualTo("Olá.");
    }

    /**
     * Test load by class and activate name.
     */
    @Test
    public void testLoadByClassAndActivateName() {
        Hello englishHello = EnhancedServiceLoader.getServiceLoader(Hello.class).load("EnglishHello");
        assertThat(englishHello.say()).isEqualTo("hello!");
    }

    /**
     * Test load by class and class loader and activate name.
     */
    @Test
    public void testLoadByClassAndClassLoaderAndActivateName() {
        Hello englishHello = EnhancedServiceLoader.getServiceLoader(Hello.class)
                .load("EnglishHello", EnhancedServiceLoaderTest.class.getClassLoader());
        assertThat(englishHello.say()).isEqualTo("hello!");
    }

    /**
     * Gets all extension class.
     */
    @Test
    public void getAllExtensionClass() {
        List<Class> allExtensionClass = EnhancedServiceLoader.getServiceLoader(Hello.class).getAllExtensionClass();
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
        List<Class> allExtensionClass = EnhancedServiceLoader.getServiceLoader(Hello.class)
                .getAllExtensionClass(ClassLoader.getSystemClassLoader());
        assertThat(allExtensionClass).isNotEmpty();
    }

    @Test
    public void getSingletonExtensionInstance(){
        Hello hello1 = EnhancedServiceLoader.getServiceLoader(Hello.class).load("ChineseHello");
        Hello hello2 = EnhancedServiceLoader.getServiceLoader(Hello.class).load("ChineseHello");
        assertThat(hello1 == hello2);
    }

    @Test
    public void getMultipleExtensionInstance(){
        Hello hello1 = EnhancedServiceLoader.getServiceLoader(Hello.class).load("LatinHello");
        Hello hello2 = EnhancedServiceLoader.getServiceLoader(Hello.class).load("LatinHello");
        assertThat(hello1 != hello2);
    }

    @Test
    public void getAllInstances(){
        List<Hello> hellows1 = EnhancedServiceLoader.getServiceLoader(Hello.class).loadAll();
        List<Hello> hellows2 = EnhancedServiceLoader.getServiceLoader(Hello.class).loadAll();
        for (Hello hello : hellows1){
            if(hello.say()!="Olá."){
                assertThat(hellows2.contains(hello));
            }
            else{
                assertThat(!hellows2.contains(hello));
            }
        }
    }

}
