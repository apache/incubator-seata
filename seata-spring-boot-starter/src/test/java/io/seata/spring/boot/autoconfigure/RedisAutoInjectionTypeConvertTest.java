package io.seata.spring.boot.autoconfigure;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import io.seata.config.FileConfiguration;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.mockito.Mockito.mock;

/**
 * @Author zhangheng
 **/
public class RedisAutoInjectionTypeConvertTest {


    @BeforeAll
    public static void initApplicationContext() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring/redis_auto_injectio_test.xml");
        applicationContext.getBeansOfType(RegistryRedisProperties.class);
    }

    @Test
    public void testRegister() throws Exception {

        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
        int db = currentConfiguration.getInt("registry.redis.db");
        Assertions.assertEquals(1,db);
    }
}
