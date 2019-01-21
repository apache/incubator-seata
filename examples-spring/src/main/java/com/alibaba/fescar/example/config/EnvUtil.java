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

    public static Environment getEnv() {
        return env;
    }

    public static void setEnv(Environment env) {
        EnvUtil.env = env;
    }

    public static String getProperty(String key) {
        return env.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    public static String getPropertyOrElseKey(String key, String orElseKey) {
        if (env.containsProperty(key)) {
            return env.getProperty(key);
        }
        return env.getProperty(orElseKey);
    }

    /**
     * 读取以propPrefix为前缀的属性Map
     *
     * @param propPrefix
     * @return
     */
    public static Map<String, Object> getSubProperty(String propPrefix) {
        if (!propPrefix.endsWith(".")) {
            propPrefix = propPrefix + ".";
        }
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, propPrefix);
        //解析${...}中引用的属性
        Map<String, Object> props = propertyResolver.getSubProperties("");
        Map<String, Object> resolved = new HashMap<String, Object>();
        resolved.putAll(props);
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof String) {
                Object replaced = resolveProps(entry.getValue().toString());
                resolved.put(entry.getKey(), replaced);
            }
        }
        return resolved;
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


    private static String resolveProps(String props) {
        Matcher matcher = propPattern.matcher(props);
        matcher.reset();
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String propName = matcher.group(1);
            String target = env.getProperty(propName, propName);
            matcher.appendReplacement(buffer, target);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static final Pattern propPattern = Pattern.compile("\\$\\{([^{]*)\\}", Pattern.DOTALL);


}
