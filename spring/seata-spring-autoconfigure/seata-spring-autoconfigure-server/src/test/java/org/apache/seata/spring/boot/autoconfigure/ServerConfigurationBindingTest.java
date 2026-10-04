/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerRateLimitProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.ServerRecoveryProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.filter.ServerHttpFilterXssProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.server.session.SessionProperties;
import org.apache.seata.spring.boot.autoconfigure.provider.SpringBootConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.common.DefaultValues.DEFAULT_END_STATUS_RETRY_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerConfigurationBindingTest {

    private final MockEnvironment environment = new MockEnvironment();
    private Object previousEnvironment;
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        previousEnvironment = ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        SeataServerEnvironmentPostProcessor.init();
        configuration = new SpringBootConfigurationProvider().provide(new FileConfiguration());
    }

    @AfterEach
    void tearDown() {
        ObjectHolder.INSTANCE.setObject(
                OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT,
                previousEnvironment == null ? new MockEnvironment() : previousEnvironment);
    }

    @Test
    void serverOptionsBindAndAreReadByCanonicalConfigurationKeys() {
        environment
                .withProperty("seata.server.rollback-failed-unlock-enable", "true")
                .withProperty("seata.server.rollback-retry-timeout-unlock-enable", "false")
                .withProperty("seata.server.distributed-lock-expire-time", "12345")
                .withProperty("seata.server.end-state-retry-dead-threshold", "23456");

        ServerProperties properties = Binder.get(environment)
                .bind("seata.server", ServerProperties.class)
                .get();

        assertTrue(properties.getRollbackFailedUnlockEnable());
        assertFalse(properties.getRollbackRetryTimeoutUnlockEnable());
        assertEquals(12345L, properties.getDistributedLockExpireTime());
        assertEquals(23456, properties.getEndStateRetryDeadThreshold());
        assertTrue(configuration.getBoolean(ConfigurationKeys.ROLLBACK_FAILED_UNLOCK_ENABLE));
        assertFalse(configuration.getBoolean(ConfigurationKeys.ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE));
        assertEquals(12345L, configuration.getLong(ConfigurationKeys.DISTRIBUTED_LOCK_EXPIRE_TIME));
        assertEquals(23456, configuration.getInt(ConfigurationKeys.END_STATE_RETRY_DEAD_THRESHOLD));
    }

    @Test
    void recoveryPeriodUsesTheRuntimeEndstatusKey() {
        environment.withProperty("seata.server.recovery.endstatus-retry-period", "1234");

        ServerRecoveryProperties properties = Binder.get(environment)
                .bind("seata.server.recovery", ServerRecoveryProperties.class)
                .get();

        assertEquals(1234L, properties.getEndstatusRetryPeriod());
        assertEquals(1234L, configuration.getLong(ConfigurationKeys.END_STATUS_RETRY_PERIOD));
    }

    @Test
    void oldEndStatusSpellingDoesNotReplaceTheRuntimeDefault() {
        environment.withProperty("seata.server.recovery.end-status-retry-period", "1234");

        assertEquals(
                String.valueOf(DEFAULT_END_STATUS_RETRY_PERIOD),
                configuration.getConfig(ConfigurationKeys.END_STATUS_RETRY_PERIOD));
    }

    @Test
    void rateLimitInitialTokensBindToTheRuntimeProperty() {
        environment
                .withProperty("seata.server.ratelimit.bucket-token-initial-num", "123")
                .withProperty("seata.server.ratelimit.bucket-token-initial-time", "456");

        ServerRateLimitProperties properties = Binder.get(environment)
                .bind("seata.server.ratelimit", ServerRateLimitProperties.class)
                .get();

        assertEquals(123, properties.getBucketTokenInitialNum());
        assertEquals(456, properties.getBucketTokenInitialTime());
        assertEquals(123, configuration.getInt(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM));
    }

    @Test
    void legacyInitialTimeDoesNotPopulateInitialTokenNumber() {
        environment.withProperty("seata.server.ratelimit.bucket-token-initial-time", "456");

        ServerRateLimitProperties properties = Binder.get(environment)
                .bind("seata.server.ratelimit", ServerRateLimitProperties.class)
                .get();

        assertEquals(456, properties.getBucketTokenInitialTime());
        assertNull(properties.getBucketTokenInitialNum());
    }

    @Test
    void branchAsyncRemoveBindsUsingTheRuntimePropertyName() {
        environment.withProperty("seata.server.session.enable-branch-async-remove", "true");

        SessionProperties properties = Binder.get(environment)
                .bind("seata.server.session", SessionProperties.class)
                .get();

        assertTrue(properties.getEnableBranchAsyncRemove());
        assertTrue(properties.getEnableBranchAsync());
        assertTrue(configuration.getBoolean(ConfigurationKeys.ENABLE_BRANCH_ASYNC_REMOVE));
    }

    @Test
    void xssKeywordsAreLoadedFromYamlAsAJsonString() throws IOException {
        String keywords = "[\"<script>\", \"custom-keyword\"]";
        String yaml =
                "seata:\n  server:\n    http:\n      filter:\n        xss:\n          keywords: '" + keywords + "'\n";
        environment
                .getPropertySources()
                .addFirst(new YamlPropertySourceLoader()
                        .load("xssKeywords", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)))
                        .get(0));

        ServerHttpFilterXssProperties properties = Binder.get(environment)
                .bind("seata.server.http.filter.xss", ServerHttpFilterXssProperties.class)
                .get();

        assertEquals(keywords, properties.getKeywords());
        assertEquals(keywords, configuration.getConfig(ConfigurationKeys.SERVER_HTTP_FILTER_XSS_FILTER_KEYWORDS));
    }
}
