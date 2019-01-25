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

package com.alibaba.fescar.test.client;

import com.alibaba.fescar.rm.RMClientAT;
import com.alibaba.fescar.test.common.ApplicationKeeper;
import com.alibaba.fescar.tm.TMClient;
import com.alibaba.fescar.tm.api.TransactionalExecutor;
import com.alibaba.fescar.tm.api.TransactionalTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class AppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppTest.class);

    private static final String APPLICATION_ID = "my_test_app";
    private static final String TX_SERVICE_GROUP = "my_test_tx_group";

    public static void main(String[] args) {
        TMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
        RMClientAT.init(APPLICATION_ID, TX_SERVICE_GROUP);

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "basic-test-context.xml");

        final JdbcTemplate jdbcTemplate = (JdbcTemplate) context
                .getBean("jdbcTemplate");

        jdbcTemplate.update("delete from undo_log");
        jdbcTemplate.update("update user0 set name = 'yyy' where id = 1");
        jdbcTemplate.update("delete from user0 where id = 2");
        jdbcTemplate.update("delete from user0 where id = 3");
        jdbcTemplate.update("insert into user0 (id, name, gmt) values (3, '2bd', '2019-01-01')");
        jdbcTemplate.update("delete from user1");

        // 0.1 prepare for the template instance
        TransactionalTemplate transactionalTemplate = new TransactionalTemplate();

        try {
            // run you business in template
//            transactionalTemplate.execute(new TransactionalExecutor() {
//                @Override
//                public Object execute() throws Throwable {
//                    LOGGER.info("Normal Committing Business Begin ...");
//                    jdbcTemplate.update("update user0 set name = 'xxx' where id = ?", new Object[]{1});
//                    return null;
//                }
//
//                @Override
//                public int timeout() {
//                    return 10000;
//                }
//
//                @Override
//                public String name() {
//                    return "my_tx_instance";
//                }
//            });

            transactionalTemplate.execute(new TransactionalExecutor() {
                @Override
                public Object execute() throws Throwable {
                    LOGGER.info("Exception Rollback Business Begin ...");
                    jdbcTemplate.update("update user0 set name = 'xxx' where id = ?", new Object[] {1});
//                    jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, now())", new Object[] {2, "abc"});
//                    jdbcTemplate.update("insert into user1 (name, gmt) values (?, now())", new Object[] {"abc"});
//                    jdbcTemplate.update(new PreparedStatementCreator() {
//                        @Override
//                        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
//                            PreparedStatement pst = con.prepareStatement("insert into user1 (name, gmt) values (?, now())", Statement.RETURN_GENERATED_KEYS);
//                            pst.setObject(1, "abc");
//                            return pst;
//                        }
//                    });
//                    jdbcTemplate.update("delete from user0 where id = ?", new Object[] {3});
                    throw new MyBusinessException("xxxxxx");
                }

                @Override
                public int timeout() {
                    return 60000;
                }

                @Override
                public String name() {
                    return "my_tx_instance";
                }
            });

//            transactionalTemplate.execute(new TransactionalExecutor() {
//                @Override
//                public Object execute() throws Throwable {
//                    LOGGER.info("Timeout Business Begin ...");
//                    jdbcTemplate.update("update user0 set name = 'xxx' where id = ?", new Object[] {1});
//                    Thread.sleep(20000); // Test timeout
//                    return null;
//                }
//
//                @Override
//                public int timeout() {
//                    return 10000;
//                }
//
//                @Override
//                public String name() {
//                    return "my_tx_instance";
//                }
//            });
        } catch (TransactionalExecutor.ExecutionException e) {
            TransactionalExecutor.Code code = e.getCode();
            if (code == TransactionalExecutor.Code.RollbackDone) {
                Throwable businessEx = e.getOriginalException();
                if (businessEx instanceof MyBusinessException) {
                    LOGGER.info(((MyBusinessException) businessEx).getBusinessErrorCode());
                }

            } else {
                Throwable cause = e.getCause();
                cause.printStackTrace();

            }
        }

        new ApplicationKeeper(context).keep();

    }

    private static class MyBusinessException extends Exception {

        private String businessErrorCode;

        public String getBusinessErrorCode() {
            return businessErrorCode;
        }

        public void setBusinessErrorCode(String businessErrorCode) {
            this.businessErrorCode = businessErrorCode;
        }

        public MyBusinessException(String businessErrorCode) {
            this.businessErrorCode = businessErrorCode;
        }
    }
}
