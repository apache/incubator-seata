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

package io.seata.common.holder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author liuqiufeng
 */
public class ObjectHolderTest {

    @BeforeEach
    void setUp() {
        ObjectHolder.INSTANCE.setObject("objectHolderTest", this);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testGetObjectByName() {
        Object object = ObjectHolder.INSTANCE.getObject("objectHolderTest");
        Assertions.assertNotNull(object);
    }

    @Test
    public void testGetObjectByClass() {
        Object object = ObjectHolder.INSTANCE.getObject(ObjectHolderTest.class);
        Assertions.assertNotNull(object);
    }
}
