package com.alibaba.fescar.common.loader;

import java.util.List;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
public class EnhancedServiceLoaderTest {

    @Test
    public void testLoadByClassAndClassLoader() {
        Hello load = EnhancedServiceLoader.load(Hello.class, Hello.class.getClassLoader());
        Assert.assertEquals(load.say(), "Bonjour");
    }

    @Test(expectedExceptions = EnhancedServiceNotFoundException.class)
    public void testLoadException() {
        EnhancedServiceLoaderTest load = EnhancedServiceLoader.load(EnhancedServiceLoaderTest.class);
    }


    @Test
    public void testLoadByClass() {
        Hello load = EnhancedServiceLoader.load(Hello.class);
        Assert.assertEquals(load.say(), "Bonjour");
    }

    @Test
    public void testLoadByClassAndActivateName() {
        Hello englishHello = EnhancedServiceLoader.load(Hello.class, "EnglishHello");
        Assert.assertEquals(englishHello.say(), "hello!");
    }

    @Test
    public void testLoadByClassAndClassLoaderAndActivateName() {
        Hello englishHello = EnhancedServiceLoader
            .load(Hello.class, "EnglishHello", EnhancedServiceLoaderTest.class.getClassLoader());
        Assert.assertEquals(englishHello.say(), "hello!");
    }

    @Test
    public void getAllExtensionClass() {
        List<Class> allExtensionClass = EnhancedServiceLoader.getAllExtensionClass(Hello.class);
        Assert.assertTrue(allExtensionClass.get(2).getSimpleName().equals(FrenchHello.class.getSimpleName()));
        Assert.assertTrue(allExtensionClass.get(1).getSimpleName().equals(EnglishHello.class.getSimpleName()));
        Assert.assertTrue(allExtensionClass.get(0).getSimpleName().equals(ChineseHello.class.getSimpleName()));

    }

    @Test
    public void getAllExtensionClass1() {
        List<Class> allExtensionClass = EnhancedServiceLoader
            .getAllExtensionClass(Hello.class, ClassLoader.getSystemClassLoader());
        Assert.assertTrue(!allExtensionClass.isEmpty());
    }


}