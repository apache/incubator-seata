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
package io.seata.saga.engine.db;

import io.seata.common.exception.StoreException;
import io.seata.core.context.RootContext;
import io.seata.saga.engine.StateMachineEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;

/**
 * State machine tests with db store exception
 *
 * @author anselleeyy
 */
public class StateMachineDBExceptionTests extends AbstractServerTest {

    private static StateMachineEngine stateMachineEngine;
    private static JdbcTemplate       jdbcTemplate;

    @BeforeAll
    public static void initApplicationContext() throws InterruptedException {

        startSeataServer();

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:saga/spring/statemachine_engine_db_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
        jdbcTemplate = applicationContext.getBean(JdbcTemplate.class);
    }

    @Test
    public void testStateMachineRecordFailed() {

        // drop table to mock record exception
        jdbcTemplate.execute("drop table seata_state_machine_inst");

        Assertions.assertThrows(StoreException.class,
            () -> stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>()));
        Assertions.assertNull(RootContext.getXID());

    }
}