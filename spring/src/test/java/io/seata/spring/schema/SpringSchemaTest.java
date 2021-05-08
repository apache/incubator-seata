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
package io.seata.spring.schema;


import io.seata.spring.annotation.GlobalTransactionScanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * The type spring schema test
 *
 * @author xingfudeshi@gmail.com
 */
public class SpringSchemaTest {
    private static ClassPathXmlApplicationContext applicationContext;


    @BeforeAll
    private static void init() {
        applicationContext = new ClassPathXmlApplicationContext("classpath:seata.xml");
    }

    @Test
    public void testGetGlobalTransactionScanner() {
        GlobalTransactionScanner globalTransactionScanner = applicationContext.getBean(GlobalTransactionScanner.class);
        Assertions.assertThat(globalTransactionScanner).isNotNull();
    }

    @Test
    public void testGtxTargetHolder() {
        GtxTarget gtxTarget = GtxTargetHolder.INSTANCE.find(GtxTargetType.METHOD, "doBiz");
        Assertions.assertThat(gtxTarget).isNotNull();
        Assertions.assertThat(gtxTarget.getTargetName()).isEqualTo("doBiz");
    }
}
