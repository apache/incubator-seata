package io.seata.console.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author: Sher
 * @date: 2022/6/30
 */
@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
