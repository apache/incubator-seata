package com.alibaba.fescar.example.config;

import org.springframework.beans.BeansException;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 环境配置实用类
 *
 * @author fj
 */
@Component
public class EnvUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private static Environment env;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        EnvUtil.applicationContext = applicationContext;
        EnvUtil.env = applicationContext.getBean(Environment.class);
    }


    /**
     * 获取指定属性名的子属性,并作为List返回
     *
     * @param propName
     * @return
     */
    public static List<String> getListSubProperty(String propName) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, propName);
        Map<String, Object> props = propertyResolver.getSubProperties("");
        if (props == null) {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (Object value : props.values()) {
            result.add(value.toString());
        }
        return result;
    }

}
