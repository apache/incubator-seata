package com.alibaba.fescar.springcloud.config;

import com.alibaba.fescar.springcloud.DataSourceProxyPostProcessor;
import com.alibaba.fescar.springcloud.FescarHystrixConcurrencyStrategy;
import com.alibaba.fescar.springcloud.FescarRequestInterceptor;
import com.alibaba.fescar.springcloud.TransactionContextFilter;
import com.netflix.hystrix.HystrixCommand;
import feign.Feign;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FescarSpringCloudConfiguration {

    @Bean
    public FilterRegistrationBean GlobalTransactionContextFilterRegister() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setFilter(new TransactionContextFilter());
        return filterRegistrationBean;
    }

    @Bean
    DataSourceProxyPostProcessor dataSourceProxyPostProcessor() {
        return new DataSourceProxyPostProcessor();
    }

    @Configuration
    @ConditionalOnClass(Feign.class)
    public static class FescarFeignConfiguration {
        @Bean
        RequestInterceptor fescarRequestInterceptor() {
            return new FescarRequestInterceptor();
        }
    }

    @Configuration
    @ConditionalOnClass(HystrixCommand.class)
    public static class FescarHystrixConfiguration {
        @Bean
        FescarHystrixConcurrencyStrategy fescarHystrixConcurrencyStrategy() {
            return new FescarHystrixConcurrencyStrategy();
        }
    }

}
