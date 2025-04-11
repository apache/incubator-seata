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

package org.apache.seata.discovery.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;


public class FileRegistryProviderTest {
    @Test
    public void testFileRegistryProvider(){
        FileRegistryProvider fileRegistryProvider = new FileRegistryProvider();
        Assertions.assertInstanceOf(FileRegistryServiceImpl.class, fileRegistryProvider.provide());
        Assertions.assertEquals(fileRegistryProvider.provide(), fileRegistryProvider.provide());
    }
    @Test
    public void testGetInstances(){
        FileRegistryServiceImpl instance = FileRegistryServiceImpl.getInstance();
        FileRegistryServiceImpl instance1 = FileRegistryServiceImpl.getInstance();
        Assertions.assertEquals(instance,instance1);
    }

}
