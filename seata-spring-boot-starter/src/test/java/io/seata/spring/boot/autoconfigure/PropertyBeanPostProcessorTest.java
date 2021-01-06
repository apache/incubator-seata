package io.seata.spring.boot.autoconfigure;

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xingfudeshi@gmail.com
 */
@Configuration
public class PropertyBeanPostProcessorTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext(PropertyBeanPostProcessorTest.class);
    }


    @Bean
    public SeataProperties seataProperties() {
        SeataProperties seataProperties = new SeataProperties();
        seataProperties.setApplicationId("test-id");
        return seataProperties;
    }

    @Bean
    public SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration() {
        return new SpringCloudAlibabaConfiguration();
    }

    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
