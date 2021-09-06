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

package com.demo.e2e.nacos;

import com.demo.factory.SeataTestHelperFactory;
import com.demo.factory.SeataTestHelperFactoryImpl;
import com.demo.helper.DruidJdbcHelper;

import com.demo.docker.annotation.ContainerHostAndPort;
import com.demo.docker.annotation.DockerCompose;
import com.demo.docker.E2E;
import com.demo.e2e.nacos.common.R;
import com.demo.model.HostAndPort;
import com.demo.trigger.TestTrigger;
import com.demo.util.TimeCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;


import java.io.InputStream;
import java.util.Properties;


@E2E
@Slf4j
public class NacosE2E{

    @DockerCompose({"docker/nacos-springcloud-alibaba/docker-compose.yml"})
    private DockerComposeContainer<?> compose;

    @ContainerHostAndPort(name = "consumer", port = 8081)
    private HostAndPort consumerHostPort;

    private DruidJdbcHelper druidJdbcHelper;

    private RestTemplate restTemplate = new RestTemplate();

    private String consumerUrl;

    @BeforeAll
    public void setUp() throws Exception {
        consumerUrl = "http://" + consumerHostPort.getHost() + ":" + consumerHostPort.getPort();
        // When connecting to the database, you need use the exposed port of the container itself
        String url  = "jdbc:mysql://0.0.0.0:3307/storage?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
        Properties properties = new Properties();
        InputStream is = DruidJdbcHelper.class.getClassLoader().getResourceAsStream("nacos/druid.properties");
        properties.load(is);
        properties.setProperty("url",url);

        SeataTestHelperFactory seataTestHelperFactory = new SeataTestHelperFactoryImpl();
        druidJdbcHelper = seataTestHelperFactory.druidJdbcQuery(properties);

    }

    @TestTrigger(value = 10)
    void shouldRetryOnSpecficException() throws Exception {

        String sql = "SELECT COUNT FROM storage_tbl WHERE commodity_code = 'apple';";
        int appleCountB = druidJdbcHelper.queryForOneValue(sql, Integer.class);
        restTemplate.getForEntity(consumerUrl + "/consumer/commodity/" + "apple", String.class);
        int appleCountA = druidJdbcHelper.queryForOneValue(sql, Integer.class);
        if (appleCountB - 1 != appleCountA) {
            throw new RuntimeException("The interface is called unsuccessfully! The data before the start is " + appleCountB + " The data after the start is " + appleCountA);
        }

        R r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
        Integer countBefore = (Integer) r.getData().get("count");

        // Call the wrong method and check the inventory
        TimeCountUtil.startTimeCount();
        try {
            ResponseEntity<String> forEntity = restTemplate.getForEntity(consumerUrl + "/consumer/commodityWrong/" + "banana", String.class);
        } catch (HttpServerErrorException e) {
            log.info("An error occurred within the consumer service");
        }
        TimeCountUtil.stopTimeCount();

        r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
        Integer countAfter = (Integer) r.getData().get("count");


        // Compare whether the inventory number has changed
        if (!countBefore.equals(countAfter)) {
            throw new RuntimeException("Unsuccessful rollback! The data before the start is " + countBefore + ", the data after the start is " + countAfter);
        } else {
            log.info("The data is unchanged and successfully rollback");
        }

    }
}