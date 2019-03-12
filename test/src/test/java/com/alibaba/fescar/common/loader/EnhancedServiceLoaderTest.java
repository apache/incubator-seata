/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

