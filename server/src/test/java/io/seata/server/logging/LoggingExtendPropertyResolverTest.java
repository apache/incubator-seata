package io.seata.server.logging;

import io.seata.server.SpringBootIntegrationTest;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author wlx
 * @date 2022/6/11 9:03 上午
 */
public class LoggingExtendPropertyResolverTest extends SpringBootIntegrationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    private LoggingExtendPropertyResolver propertyResolver;

    @BeforeEach
    void before() {
        propertyResolver = new LoggingExtendPropertyResolver(environment);
    }

    @Test
    void getPropertyMapByPrefixTest() {
        System.setProperty("logging.extend.kafka-appender.producer-configs.acks", "0");
        System.setProperty("logging.extend.kafka-appender.producer-configs.linger.ms", "1000");
        System.setProperty("logging.extend.kafka-appender.producer-configs.max.block.ms", "0");
        Map<String, Object> propertyMapByPrefix = propertyResolver.getPropertyMapByPrefix("logging.extend.kafka-appender.producer-configs");
        Map<String, Object> expected = new HashMap<>();
        expected.put("acks", "0");
        expected.put("linger.ms", "1000");
        expected.put("max.block.ms", "0");
        assertEquals(expected, propertyMapByPrefix);
    }

    @Test
    void resolvePlaceholdersTest() {
        String appName = propertyResolver.getProperty("spring.application.name", "seataServer");
        String rpcPort = propertyResolver.getProperty("server.servicePort", "0");
        String format = "%s:%s";
        String expected = String.format(format, appName, rpcPort);
        String text = "${spring.application.name:seataServer}:${server.servicePort:0}";
        String resolveText = propertyResolver.resolvePlaceholders(text);
        assertEquals(expected, resolveText);

    }
}
