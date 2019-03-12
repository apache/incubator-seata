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
package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;

import org.junit.Assert;
import org.junit.Test;

/**
 * ConfigType test.
 *
 * @author Lay
 */
public class ConfigTypeTest {

    /**
     * Test get type with file.
     */
    @Test
    public void testGetTypeWithFile() {
        ConfigType configType = ConfigType.getType("file");
        Assert.assertEquals(configType, ConfigType.File);
    }

    /**
     * Test get type with nacos.
     */
    @Test
    public void testGetTypeWithNacos() {
        ConfigType configType = ConfigType.getType("nacos");
        Assert.assertEquals(configType, ConfigType.Nacos);
    }

    /**
     * Test get type with apollo.
     */
    @Test
    public void testGetTypeWithApollo() {
        ConfigType configType = ConfigType.getType("apollo");
        Assert.assertEquals(configType, ConfigType.Apollo);
    }

    /**
     * Test get type with not support.
     */
    @Test(expected = NotSupportYetException.class)
    public void testGetTypeWithNotSupport() {
        ConfigType.getType("none");
    }
}
