package org.apache.seata.server.spring.web;

import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

@Component
public class RestControllerBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!bean.getClass().isAnnotationPresent(RestController.class)) {
            return bean;
        }
        HttpDispatchHandler.addHttpController(bean);
        return bean;
    }
}
