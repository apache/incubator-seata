package io.seata.spring.boot.autoconfigure;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;

/**
 * @description:
 * @author: zhangjiawei
 * @date: 2024/1/2
 */
public class SeataClientEnvironmentPostProcessorTest {

    @Test
    public void testSeataClientEnvironmentPostProcessor() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext("io.seata.spring.boot.autoconfigure.properties");

        SeataClientEnvironmentPostProcessor seataClientEnvironmentPostProcessor = new SeataClientEnvironmentPostProcessor();
        seataClientEnvironmentPostProcessor.postProcessEnvironment(applicationContext.getEnvironment(), null);
        Assertions.assertEquals(Ordered.HIGHEST_PRECEDENCE, seataClientEnvironmentPostProcessor.getOrder());
    }
}
