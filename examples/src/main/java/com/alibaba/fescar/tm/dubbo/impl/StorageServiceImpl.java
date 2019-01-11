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

package com.alibaba.fescar.tm.dubbo.impl;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.RMClientAT;
import com.alibaba.fescar.test.common.ApplicationKeeper;
import com.alibaba.fescar.tm.dubbo.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
public class StorageServiceImpl implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deduct(String commodityCode, int count) {
        LOGGER.info("Storage Service Begin ... xid: " + RootContext.getXID());
        jdbcTemplate.update("update storage_tbl set count = count - ? where commodity_code = ?", new Object[] {count, commodityCode});
        LOGGER.info("Storage Service End ... ");

    }

    public static void main(String[] args) throws Throwable {

        String applicationId = "dubbo-demo-storage-service";
        String txServiceGroup = "my_test_tx_group";

        RMClientAT.init(applicationId, txServiceGroup);

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-storage-service.xml"});
        context.getBean("service");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");
        jdbcTemplate.update("delete from storage_tbl where commodity_code = 'C00321'");
        jdbcTemplate.update("insert into storage_tbl(commodity_code, count) values ('C00321', 100)");
        new ApplicationKeeper(context).keep();
    }
}
