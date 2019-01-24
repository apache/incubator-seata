package com.alibaba.fescar.springcloud;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class DataSourceProxyPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DruidDataSource)) {
            return bean;
        }
        DruidDataSource druidDataSource = (DruidDataSource) bean;
        DataSourceProxy dataSourceProxy = new DataSourceProxy(druidDataSource);
        return dataSourceProxy;
    }
}
