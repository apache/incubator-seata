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

package seata.e2e.nacos;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import seata.e2e.factory.SeataTestHelperFactory;
import seata.e2e.factory.impl.SeataTestHelperFactoryImpl;
import seata.e2e.helper.*;
import seata.e2e.docker.annotation.ContainerHostAndPort;
import seata.e2e.docker.annotation.DockerCompose;
import seata.e2e.docker.E2E;
import seata.e2e.nacos.common.R;
import seata.e2e.model.HostAndPort;
import seata.e2e.trigger.TestTrigger;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;

/**
 * @author jingliu_xiong@foxmail.com
 */
@E2E
public class NacosE2E{

    private static final Logger LOGGER = LoggerFactory.getLogger(CronTask.class);

    @DockerCompose({"docker/nacos-springcloud-alibaba/docker-compose.yml"})
    private DockerComposeContainer compose;

    @ContainerHostAndPort(name = "consumer", port = 8081)
    private HostAndPort consumerHostPort;

    private DruidJdbcHelper druidJdbcHelper;

    private RestTemplate restTemplate = new RestTemplate();

    private SeataTestHelperFactory seataTestHelperFactory;

    private String consumerUrl;

    @BeforeAll
    public void setUp() throws Exception {
        consumerUrl = "http://" + consumerHostPort.getHost() + ":" + consumerHostPort.getPort();
        // When connecting to the database, you need use the exposed port of the container itself
        String url  = "jdbc:mysql://localhost:3307/storage?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
        Properties properties = new Properties();
        InputStream is = DruidJdbcHelper.class.getClassLoader().getResourceAsStream("nacos-springcloud-alibaba-test/druid.properties");
        properties.load(is);
        properties.setProperty("url",url);

        seataTestHelperFactory = new SeataTestHelperFactoryImpl();
        druidJdbcHelper = seataTestHelperFactory.druidJdbcQuery(properties);
    }

    @TestTrigger(value = 10)
    public void testStartUp() {
        LOGGER.info("start up success");
    }

    @TestTrigger(value = 10)
    public void shouldRetryOnAnyException(){

//        String sql = "SELECT COUNT FROM storage_tbl WHERE commodity_code = 'apple';";
//        int appleCountB = druidJdbcHelper.queryForOneValue(sql, Integer.class);
        // Apple inventory minus 1
//        restTemplate.getForEntity(consumerUrl + "/consumer/commodity/" + "apple", String.class);
//        int appleCountA = druidJdbcHelper.queryForOneValue(sql, Integer.class);
//        if (appleCountB - 1 != appleCountA) {
//            throw new RuntimeException("The interface is called unsuccessfully! The data before the start is " + appleCountB + " The data after the start is " + appleCountA);
//        }
//
//        sql = "SELECT COUNT FROM storage_tbl WHERE commodity_code = 'banana';";
//        Integer countBefore = druidJdbcHelper.queryForOneValue(sql, Integer.class);


        R r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
        Integer countBefore = (Integer) r.getData().get("count");

        TimeCountHelper timeCountHelper = seataTestHelperFactory.timeCountHelper();
        timeCountHelper.startTimeCount();
        try {
            // Call a method with the @globaltransactional annotation that will make an exception during execution.
            ResponseEntity<String> forEntity = restTemplate.getForEntity(consumerUrl + "/consumer/commodityWrong/" + "banana", String.class);
        } catch (HttpServerErrorException e) {
            LOGGER.info("an error occurred within the consumer service");
        }
        long l = timeCountHelper.stopTimeCount();
        // Time spent accessing this interface
        LOGGER.info("request cost times: {} s", l / 1000);

        // Query banana inventory
        r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
        Integer countAfter = (Integer) r.getData().get("count");

        // Compare whether the inventory has changed

        if (!countBefore.equals(countAfter)) {
            throw new RuntimeException("Unsuccessful rollback! The data before the start is " + countBefore + ", the data after the start is " + countAfter);
        } else {
            LOGGER.info("The data is unchanged and successfully rollback");
        }

    }

    @TestTrigger(value = 10)
    public void pressureTaskWithJudgerTest() {

        PressureTask pressureTask = seataTestHelperFactory.pressureController(() -> {
            // Query banana inventory
            ResponseEntity<R> r = restTemplate.getForEntity(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
            HttpStatus statusCode = r.getStatusCode();
            return r;
        }, 100, Runtime.getRuntime().availableProcessors());
        pressureTask.start(true, r -> {
            // Verify the response
            if (r == null) throw new RuntimeException("Presurre Test failed!");
            ResponseEntity<R> responseEntity = (ResponseEntity<R>) r;
            HttpStatus status = responseEntity.getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new RuntimeException("Presurre Test failed!");
            }
            return true;
        });
    }

    @TestTrigger(value = 1)
    public void pressureTaskTest() {

        PressureTask pressureTask = seataTestHelperFactory.pressureController(() -> {
            ResponseEntity<R> r = restTemplate.getForEntity(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
            HttpStatus statusCode = r.getStatusCode();
            return r;
        }, 100, Runtime.getRuntime().availableProcessors());
        // Not Verify the response
        pressureTask.start(false);

    }



    @TestTrigger(value = 1)
    public void timesTaskTest() {

        TimesTask timesTask = seataTestHelperFactory.timseTask(() -> {
            R r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
            Integer count = (Integer) r.getData().get("count");
            LOGGER.info("There are " + count + " bananas");
            return r;
        }, 100, 1000);
        timesTask.start();

    }

    @TestTrigger(value = 10)
    public void cronTaskTest() throws InterruptedException {

        String sql = "SELECT COUNT FROM storage_tbl WHERE commodity_code = 'banana';";
        Integer countBefore = druidJdbcHelper.queryForOneValue(sql, Integer.class);
        CronTask cronTask = seataTestHelperFactory.cronTask(1000, () -> {
            try {
                ResponseEntity<String> forEntity = restTemplate.getForEntity(consumerUrl + "/consumer/commodityWrong/" + "banana", String.class);
            } catch (HttpServerErrorException e) {
                LOGGER.info("An error occurred within the consumer service");
            }
            return null;
        });
        cronTask.start();
        Thread.sleep(5000);
        cronTask.stop();
        Integer countAfter = druidJdbcHelper.queryForOneValue(sql, Integer.class);
        // Compare whether the inventory number has changed
        if (!countBefore.equals(countAfter)) {
            throw new RuntimeException("Unsuccessful rollback! The data before the start is " + countBefore + ", the data after the start is " + countAfter);
        } else {
            LOGGER.info("The data is unchanged but may test fail");
        }
    }


}