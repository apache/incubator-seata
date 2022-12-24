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
package io.seata.config.nacos;

import java.lang.reflect.Method;
import java.util.Properties;

import io.seata.common.util.ReflectionUtil;
import io.seata.discovery.registry.nacos.NacosRegistryServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * The type Nacos registry serivce impl test
 *
 * @author xingfudeshi@gmail.com
 */
public class NacosRegistryServiceImplTest {

    @Test
    public void testGetConfigProperties() throws Exception {
        Method method = ReflectionUtil.getMethod(NacosRegistryServiceImpl.class, "getNamingProperties");
        Properties properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        Assertions.assertThat(properties.getProperty("contextPath")).isEqualTo("/foo");
        System.setProperty("contextPath", "/bar");
        properties = (Properties) ReflectionUtil.invokeMethod(null, method);
        Assertions.assertThat(properties.getProperty("contextPath")).isEqualTo("/bar");
    }


}
