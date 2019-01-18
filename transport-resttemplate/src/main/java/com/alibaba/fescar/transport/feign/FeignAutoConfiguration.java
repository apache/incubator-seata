package com.alibaba.fescar.transport.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Loki
 */
@Configuration
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    public RequestInterceptor feignClientRequestInterceptor() {
        return new FeignClientRequestInterceptor();
    }
}
