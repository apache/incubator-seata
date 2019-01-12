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

package com.alibaba.fescar.test;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.rm.datasource.DataSourceManager;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class DataSourceBasicTest {
    public void runBusiness(JdbcTemplate jdbcTemplate) {
//        jdbcTemplate.update("insert into user0 (id, name, gmt) values (?, ?, ?)",
//                new Object[] { 1, "xxx", new Date() });
        jdbcTemplate.update("update user0 set name = 'yyyy' where id = ?", new Object[] {1});
//        jdbcTemplate.update("delete from user0 where id = ?", new Object[] {1});


    }

    public static void main(String[] args) {
        DataSourceManager.set(new DataSourceManager() {

            @Override
            public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String lockKeys) throws TransactionException {
                return 123456L;
            }

            @Override
            public void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {

            }

            @Override
            public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
                return true;
            }

            @Override
            public void registerResource(Resource resource) {

            }

            @Override
            public void unregisterResource(Resource resource) {

            }

            @Override
            public BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
                return BranchStatus.PhaseTwo_Committed;
            }

            @Override
            public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        });

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "basic-test-context.xml");
        final DataSourceBasicTest clientTest = (DataSourceBasicTest) context
                .getBean("clientTest");
        final JdbcTemplate jdbcTemplate = (JdbcTemplate) context
                .getBean("jdbcTemplate");
        final JdbcTemplate directJdbcTemplate = (JdbcTemplate) context
                .getBean("directJdbcTemplate");
//        directJdbcTemplate.execute("truncate table user0");

        RootContext.bind("test_xid");
        clientTest.runBusiness(jdbcTemplate);

        context.close();
        System.exit(0);
    }
}
