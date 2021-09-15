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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author slievrly
 */
public class BaseConfigPropertiesTest {
    protected static AnnotationConfigApplicationContext applicationContex;
    protected static final String STR_TEST_AAA = "aaa";
    protected static final String STR_TEST_BBB = "bbb";
    protected static final String STR_TEST_CCC = "ccc";
    protected static final String STR_TEST_DDD = "ddd";
    protected static final String STR_TEST_EEE = "eee";
    protected static final String STR_TEST_FFF = "fff";

    @BeforeAll
    public static void setUp() {
        applicationContex = new AnnotationConfigApplicationContext(
            new String[] {"io.seata.spring.boot.autoconfigure.properties.test"});
        SeataCoreEnvironmentPostProcessor processor = new SeataCoreEnvironmentPostProcessor();
        processor.postProcessEnvironment(null, null);
    }

    @AfterAll
    public static void tearDown() {
        if (null != applicationContex) {
            applicationContex.close();
        }
    }

}