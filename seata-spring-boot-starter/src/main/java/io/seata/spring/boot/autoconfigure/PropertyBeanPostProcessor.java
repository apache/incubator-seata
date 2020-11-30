package io.seata.spring.boot.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;

/**
 * @author xingfudeshi@gmail.com
 */
public class PropertyBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(ConfigurationProperties.class)) {
            ConfigurationProperties configurationProperties = bean.getClass().getAnnotation(ConfigurationProperties.class);
            String prefix = configurationProperties.prefix();
            PROPERTY_BEAN_MAP.computeIfPresent(prefix, (k, v) -> {
                v.complete(bean);
                return v;
            });
        }
        return bean;
    }
}
