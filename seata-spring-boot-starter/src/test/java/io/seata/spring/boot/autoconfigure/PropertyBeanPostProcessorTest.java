package io.seata.spring.boot.autoconfigure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Configuration
public class PropertyBeanPostProcessorTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        PROPERTY_BEAN_MAP.putIfAbsent(SEATA_PREFIX, new CompletableFuture<>());
        context = new AnnotationConfigApplicationContext(PropertyBeanPostProcessorTest.class);
    }

    @Bean
    public PropertyBeanPostProcessor propertyBeanPostProcessor() {
        return new PropertyBeanPostProcessor();
    }

    @Bean
    public SeataProperties seataProperties() {
        SeataProperties seataProperties=new SeataProperties();
        seataProperties.setApplicationId("test-id");
        return seataProperties;
    }

    @Bean
    public SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration() {
        return new SpringCloudAlibabaConfiguration();
    }

    @Test
    public void testCompletePropertyBean() throws ExecutionException, InterruptedException, TimeoutException {
        Object object=PROPERTY_BEAN_MAP.get(SEATA_PREFIX).get(3, TimeUnit.SECONDS);
        Assertions.assertThat(object).isInstanceOf(SeataProperties.class);
        SeataProperties seataProperties= (SeataProperties) object;
        Assertions.assertThat(seataProperties.getApplicationId()).isEqualTo("test-id");
    }


    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
