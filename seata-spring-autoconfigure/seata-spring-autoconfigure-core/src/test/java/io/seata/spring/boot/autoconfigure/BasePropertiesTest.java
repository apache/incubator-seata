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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.seata.common.holder.ObjectHolder;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

/**
 * @author slievrly
 */
public class BasePropertiesTest {
    protected static AnnotationConfigApplicationContext applicationContex;
    protected static final String STR_TEST_AAA = "aaa";
    protected static final String STR_TEST_BBB = "bbb";
    protected static final String STR_TEST_CCC = "ccc";
    protected static final String STR_TEST_DDD = "ddd";
    protected static final String STR_TEST_EEE = "eee";
    protected static final String STR_TEST_FFF = "fff";

    protected static final int LONG_TEST_ONE = 1;
    protected static final int LONG_TEST_TWO = 2;

    @BeforeEach
    public void setUp() throws IOException {
        applicationContex = new AnnotationConfigApplicationContext(
            new String[] {"io.seata.spring.boot.autoconfigure.properties.config.test"});
        SeataCoreEnvironmentPostProcessor processor = new SeataCoreEnvironmentPostProcessor();
        processor.postProcessEnvironment(null, null);

        // set new applicationContex for test cases in extension test classes
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT, applicationContex);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, applicationContex.getEnvironment());
        Properties properties=new Properties();
        ClassLoader classLoader = getClass().getClassLoader();
        File f = new File(classLoader.getResource("application-test.properties").getFile());
        try(InputStream in =new FileInputStream(f)) {
            properties.load(in);
        }
        applicationContex.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("serverProperties", properties));

    }

    @AfterEach
    public void closeContext() {
        if(applicationContex!=null) {
            applicationContex.close();
        }
    }

}