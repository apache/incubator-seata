package com.alibaba.fescar.common.loader;

import com.alibaba.fescar.common.loader.EnhancedServiceLoader;
import com.alibaba.fescar.common.loader.LoaderTestSPI;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * test  EnhancedServiceLoader
 * @author zhangsen
 */
public class EnhancedServiceLoaderTest {

    @Test
    public void testLoadBeanByOrder(){
        LoaderTestSPI loader  = EnhancedServiceLoader.load(LoaderTestSPI.class, EnhancedServiceLoaderTest.class.getClassLoader());
        System.out.println(loader.echo());
        Assert.assertEquals("impl_2", loader.echo());
    }

    @Test
    public void testLoadAll(){
        List<LoaderTestSPI> list = EnhancedServiceLoader.loadAll(LoaderTestSPI.class);
        Assert.assertEquals(2, list.size());
    }


}

