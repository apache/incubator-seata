package io.seata.spring.boot.autoconfigure.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * @description:
 * @author: zhangjiawei
 * @date: 2024/1/2
 */
public class SpringCloudAlibabaConfigurationTest {

    @Test
    public void testSpringCloudAlibabaConfiguration() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");
        Properties properties = new Properties();
        properties.setProperty("spring.application.name", "test");
        applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("my_test", properties));

        SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration = (SpringCloudAlibabaConfiguration) applicationContext.getBean("springCloudAlibabaConfiguration");

        // application id is null
        Assertions.assertEquals("test", springCloudAlibabaConfiguration.getApplicationId());
        // application is not null
        Assertions.assertEquals("test", springCloudAlibabaConfiguration.getApplicationId());
        Assertions.assertEquals("default_tx_group", springCloudAlibabaConfiguration.getTxServiceGroup());
        springCloudAlibabaConfiguration.setTxServiceGroup("default_tx_group_1");
        Assertions.assertEquals("default_tx_group_1", springCloudAlibabaConfiguration.getTxServiceGroup());
        applicationContext.close();
    }
}
