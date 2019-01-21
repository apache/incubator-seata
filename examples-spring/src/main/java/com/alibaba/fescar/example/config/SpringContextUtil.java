package com.alibaba.fescar.example.config;


import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * spring上下文实用类
 *
 * @author fj
 */
@Component
@Lazy(false)
@Order(HIGHEST_PRECEDENCE)
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    private static BeanDefinitionRegistry beanDefinitionRegistry;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.context = applicationContext;
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) context;
        beanDefinitionRegistry = (BeanDefinitionRegistry) configurableApplicationContext.getBeanFactory();
    }


    public static ApplicationContext getContext() {
        return context;
    }

    public static Object getBean(String name) {
        return context.getBean(name);
    }

    public static <T> T getBean(Class<T> clz) {
        return context.getBean(clz);
    }

    public static <T> T getBean(String name, Class<T> clz) {
        return context.getBean(name, clz);
    }

    /**
     * 获取或注册一个clz类型的bean
     *
     * @param clz bean类型
     * @param <T>
     * @return
     */
    public static <T> T getOrRegisterBean(Class<T> clz) {
        Map<String, T> beans = SpringContextUtil.getContext().getBeansOfType(clz);
        if (beans.size() == 0) {
            String beanName = getRegisterBeanName(clz);
            SpringContextUtil.registerBean(beanName, clz);
            return getBean(beanName, clz);
        }
        String beanName = beans.keySet().iterator().next();
        return beans.get(beanName);
    }

    /**
     * 动态注册bean,指定bean名
     *
     * @param beanName
     * @param beanDefinition
     */
    public synchronized static void registerBean(String beanName, BeanDefinition beanDefinition) {
        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    /**
     * 动态注册bean,bean名为第一个字母小写的类名
     *
     * @param clz
     */
    public static void registerBean(Class clz) {
        String simpleNameString = getRegisterBeanName(clz);
        registerBean(simpleNameString, clz);
    }

    /**
     * 动态注册bean为特定bean名
     *
     * @param clz
     */
    public static void registerBean(String beanName, Class clz) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clz);
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        registerBean(beanName, beanDefinition);
    }

    /**
     * 获取当前代理对象
     *
     * @return
     */
    public static <T> T currProxyObj() {
        return (T) AopContext.currentProxy();
    }


    private static String getRegisterBeanName(Class clz) {
        String simpleNameString = clz.getSimpleName();
        simpleNameString = simpleNameString.substring(0, 1).toString() + simpleNameString.substring(1);
        return simpleNameString;
    }
}
