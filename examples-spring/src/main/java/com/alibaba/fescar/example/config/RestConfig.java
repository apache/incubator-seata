package com.alibaba.fescar.example.config;

import com.alibaba.fescar.example.interceptor.TxRestTemplateInterceptor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
public class RestConfig {
    @Autowired
    Environment env;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) template.getRequestFactory();

        //设置默认字符集为UTF8
        List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        for (Integer i = 0; i < converters.size(); i++) {
            HttpMessageConverter converter = converters.get(i);
            if (converter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) converter).setDefaultCharset(Charset.forName("UTF-8"));
            }
        }

        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "restTemplate.requestFactory");
        Map<String, Object> params = propertyResolver.getSubProperties("");
        MutablePropertyValues paramsProps = new MutablePropertyValues(params);

        RelaxedDataBinder dataBinder = new RelaxedDataBinder(factory);
        ConversionService conversionService = new DefaultConversionService();
        dataBinder.setConversionService(conversionService);
        dataBinder.setIgnoreNestedProperties(false);
        dataBinder.setIgnoreInvalidFields(false);
        dataBinder.setIgnoreUnknownFields(true);
        dataBinder.bind(paramsProps);

        TxRestTemplateInterceptor transactionInterceptor = new TxRestTemplateInterceptor();
        template.setInterceptors(Collections.singletonList(transactionInterceptor));
        return template;
    }
}
