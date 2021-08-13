package com.demo.e2e.nacos;

import com.demo.adapter.DruidJdbcQuery;
import com.demo.adapter.SeataTestAdapter;
import com.demo.docker.annotation.ContainerHostAndPort;
import com.demo.docker.annotation.DockerCompose;
import com.demo.e2e.SeataE2E;
import com.demo.e2e.nacos.common.R;
import com.demo.model.HostAndPort;
import com.demo.trigger.TestTrigger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.InputStream;
import java.util.Properties;


@Slf4j
@SeataE2E
public class NacosE2E extends SeataTestAdapter {

    @DockerCompose({"docker/nacos-springcloud-alibaba/docker-compose.yml"})
    private DockerComposeContainer<?> compose;

    @ContainerHostAndPort(name = "consumer", port = 8081)
    private HostAndPort consumerHostPort;


    private String consumerUrl;

    @BeforeAll
    public void setUp() throws Exception {
        consumerUrl = "http://" + consumerHostPort.host() + ":" + consumerHostPort.port();
        // When connecting to the database, you need use the exposed port of the container itself
        String url  = "jdbc:mysql://0.0.0.0:3306/storage?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
        Properties properties = new Properties();
        InputStream is = DruidJdbcQuery.class.getClassLoader().getResourceAsStream("nacos/druid.properties");
        properties.load(is);
        properties.setProperty("url",url);
        druidJdbcQuery(properties);
    }

    @TestTrigger(value = 1)
    void shouldRetryOnSpecficException() throws Exception {

        String sql = "SELECT COUNT FROM storage_tbl WHERE commodity_code = 'apple';";
        int appleCountB = druidJdbcQuery.queryForOneValue(sql, Integer.class);
        restTemplate.getForEntity(consumerUrl + "/consumer/commodity/" + "apple", String.class);
        int appleCountA = druidJdbcQuery.queryForOneValue(sql, Integer.class);
        if (appleCountB - 1 != appleCountA) {
            throw new RuntimeException("The interface is called unsuccessfully! The data before the start is " + appleCountB + " he data after the start is " + appleCountA);
        }

        R r = restTemplate.getForObject(consumerUrl + "/consumer/" + "banana" + "/count", R.class);
        Integer countBefore = (Integer) r.getData().get("count");

        // Call the wrong method and check the inventory
        try {
            ResponseEntity<String> forEntity = restTemplate.getForEntity(consumerUrl + "/consumer/commodityWrong/" + "banana", String.class);
        } catch (HttpServerErrorException e) {
            log.info("An error occurred within the consumer service");
        }
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
