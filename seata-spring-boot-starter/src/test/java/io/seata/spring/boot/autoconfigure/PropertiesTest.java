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
package io.seata.spring.boot.autoconfigure;

import io.seata.spring.boot.autoconfigure.properties.file.LockProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author xingfudeshi@gmail.com
 */
public class PropertiesTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");
    }

    @Test
    public void testLockProperties() {
        assertEquals(10, context.getBean(LockProperties.class).getRetryInterval());
        assertEquals(30, context.getBean(LockProperties.class).getRetryTimes());
        assertEquals(true, context.getBean(LockProperties.class).isRetryPolicyBranchRollbackOnConflict());
    }

    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
