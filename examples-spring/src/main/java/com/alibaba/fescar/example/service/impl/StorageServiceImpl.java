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

package com.alibaba.fescar.example.service.impl;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.example.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
@Service
public class StorageServiceImpl implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deduct(String commodityCode, int count) {
        LOGGER.info("Storage Service Begin ... xid: " + RootContext.getXID());
        LOGGER.info("Deducting inventory SQL: update storage_tbl set count = count - {} where commodity_code = {}", count, commodityCode);

        jdbcTemplate.update("UPDATE storage_tbl SET count = count - ? WHERE commodity_code = ?", new Object[]{count, commodityCode});
        LOGGER.info("Storage Service End ... ");

    }

}
