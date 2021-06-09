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

import io.seata.spring.proxy.SeataProxyAutoProxyCreator;
import io.seata.spring.proxy.SeataProxyConfig;
import io.seata.spring.proxy.SeataProxyHandler;
import io.seata.spring.tcc.DefaultTccSeataProxyActionImpl;
import io.seata.spring.tcc.TccSeataProxyAction;
import io.seata.spring.tcc.TccSeataProxyHandler;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Seata Proxy Auto Configuration.
 *
 * @author wang.liang
 */
@Configuration
@ConditionalOnExpression("${seata.enabled:true} && ${seata.proxy.enabled:true}")
@AutoConfigureAfter({SeataTCCAutoConfiguration.class})
public class SeataProxyAutoConfiguration {

    /**
     * The configuration for the implementation of the {@link SeataProxyHandler}
     *
     * @see io.seata.spring.proxy.SeataProxy
     * @see SeataProxyHandler
     * @see io.seata.spring.proxy.SeataProxyAutoProxyCreator
     */
    @Configuration
    @ConditionalOnMissingBean(SeataProxyHandler.class)
    static class TccSeataProxyConfiguration {

        @Bean
        @Lazy(false)
        @ConditionalOnMissingBean
        public TccSeataProxyAction defaultTccSeataProxyAction() {
            return new DefaultTccSeataProxyActionImpl();
        }

        /**
         * The {@link io.seata.spring.proxy.SeataProxyAutoProxyCreator} will use the handler.
         *
         * @return the seata proxy handler
         */
        @Bean
        public SeataProxyHandler tccSeataProxyHandler() {
            return new TccSeataProxyHandler();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(StarterConstants.PROXY_PREFIX)
    public SeataProxyConfig seataProxyConfig() {
        return new SeataProxyConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public SeataProxyAutoProxyCreator seataProxyAutoProxyCreator(SeataProxyConfig config, SeataProxyHandler seataProxyHandler) {
        return new SeataProxyAutoProxyCreator(config, seataProxyHandler);
    }
}
