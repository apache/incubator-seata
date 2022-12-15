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
package io.seata.spring.boot.autoconfigure;

import io.seata.integration.http.JakartaSeataWebMvcConfigurer;
import io.seata.integration.http.SeataWebMvcConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import static io.seata.spring.boot.autoconfigure.StarterConstants.HTTP_PREFIX;

/**
 * Auto bean add for spring webmvc if in springboot env.
 *
 * @author wangxb
 * @author wang.liang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnMissingBean(SeataWebMvcConfigurer.class)
@ConditionalOnProperty(prefix = HTTP_PREFIX, name = "interceptor-enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class SeataHttpAutoConfiguration {

    /**
     * The Jakarta seata web mvc configurer.
     *
     * @return the seata web mvc configurer
     */
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    public JakartaSeataWebMvcConfigurer jakartaSeataWebMvcConfigurer() {
        return new JakartaSeataWebMvcConfigurer();
    }

    /**
     * The Javax seata web mvc configurer.
     *
     * @return the seata web mvc configurer
     */
    @Bean
    @ConditionalOnMissingBean(JakartaSeataWebMvcConfigurer.class)
    public SeataWebMvcConfigurer seataWebMvcConfigurer() {
        return new SeataWebMvcConfigurer();
    }
}
