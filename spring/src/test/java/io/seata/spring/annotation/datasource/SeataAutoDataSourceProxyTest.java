/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.annotation.datasource;

import io.seata.core.model.BranchType;
import io.seata.rm.datasource.SeataDataSourceProxy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

/**
 * @author selfishlover
 */
class SeataAutoDataSourceProxyTest {

    private static final String PRIMARY_NAME = "primaryDataSource";

    private static SeataAutoDataSourceProxyAdvice advice;

    private static SeataAutoDataSourceProxyCreator creator;

    @BeforeAll
    static void beforeAll() {
        boolean useJdkProxy = true;
        String[] excludes = new String[0];
        String dataSourceProxyMode = BranchType.AT.name();

        advice= spy(new SeataAutoDataSourceProxyAdvice(dataSourceProxyMode));
        Object[] advices = new Object[]{new DefaultIntroductionAdvisor(advice)};

        creator = spy(new SeataAutoDataSourceProxyCreator(useJdkProxy, excludes, dataSourceProxyMode));
        doReturn(advices).when(creator).getAdvicesAndAdvisorsForBean(any(), anyString(), any());
    }

    @Configuration(proxyBeanMethods = false)
    static class ProxyConfiguration {
        @Bean
        public SeataAutoDataSourceProxyCreator seataAutoDataSourceProxyCreator() {
            return creator;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfiguration1 {

        static DataSource origin = mock(DataSource.class);

        static SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);

        @Bean(PRIMARY_NAME)
        public DataSource dataSource() {
            return origin;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfiguration2 {

        static DataSource origin = mock(DataSource.class);

        static SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);

        @Bean(PRIMARY_NAME)
        public DataSource dataSource() {
            when(proxy.getTargetDataSource()).thenReturn(origin);
            return proxy;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfiguration3 {

        static DataSource origin = mock(DataSource.class);

        static SeataDataSourceProxy proxy = mock(SeataDataSourceProxy.class);

        @Bean
        public DataSource origin() {
            return origin;
        }

        @Bean(PRIMARY_NAME)
        @Primary
        public DataSource dataSource(@Qualifier("origin") DataSource target) {
            when(proxy.getTargetDataSource()).thenReturn(target);
            return proxy;
        }
    }

    @Test
    void testAll() throws SQLException {
        testProxy(DataSourceConfiguration1.origin, DataSourceConfiguration1.proxy, DataSourceConfiguration1.class);
        verify(creator, times(1)).buildProxy(any(), anyString());

        testProxy(DataSourceConfiguration2.origin, DataSourceConfiguration2.proxy, DataSourceConfiguration2.class);
        verify(creator, times(1)).buildProxy(any(), anyString());

        testProxy(DataSourceConfiguration3.origin, DataSourceConfiguration3.proxy, DataSourceConfiguration3.class);
        verify(creator, times(2)).buildProxy(any(), anyString());
    }

    private void testProxy(DataSource origin, SeataDataSourceProxy proxy, Class<?> dataSourceConfiguration) throws SQLException {
        doReturn(proxy).when(creator).buildProxy(any(), anyString());

        ApplicationContext context = new AnnotationConfigApplicationContext(ProxyConfiguration.class, dataSourceConfiguration);
        DataSource enhancer = context.getBean(PRIMARY_NAME, DataSource.class);

        assertTrue(enhancer instanceof SeataProxy);

        assertSame(proxy, DataSourceProxyHolder.get(origin));

        doReturn(true).when(advice).inExpectedContext();

        enhancer.getConnection();
        verify(origin, never()).getConnection();
        verify(proxy, times(1)).getConnection();

        enhancer.unwrap(Object.class);
        verify(origin, times(1)).unwrap(Object.class);
        verify(proxy, never()).unwrap(Object.class);
    }
}