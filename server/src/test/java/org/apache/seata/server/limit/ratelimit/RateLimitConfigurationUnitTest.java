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
package org.apache.seata.server.limit.ratelimit;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitConfigurationUnitTest extends BaseSpringBootTest {
    @Test
    void dynamicChangesPreserveLastValidNumbers() {
        RateLimiter limiter = mock(RateLimiter.class);
        RateLimiterHandler handler = new RateLimiterHandler(limiter);
        change(handler, ConfigurationKeys.RATE_LIMIT_ENABLE, "true");
        change(handler, ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND, "1");
        change(handler, ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM, "10");
        change(handler, ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM, "2");
        change(handler, ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM, "invalid");
        org.mockito.ArgumentCaptor<RateLimiterHandlerConfig> captured =
                org.mockito.ArgumentCaptor.forClass(RateLimiterHandlerConfig.class);
        verify(limiter, times(5)).reInit(captured.capture());
        RateLimiterHandlerConfig config = captured.getValue();
        assertTrue(config.isEnable());
        assertEquals(1, config.getBucketTokenNumPerSecond());
        assertEquals(10, config.getBucketTokenMaxNum());
        assertEquals(2, config.getBucketTokenInitialNum());
        TokenBucketLimiter bucket = new TokenBucketLimiter();
        bucket.reInit(config);
        assertTrue(bucket.isEnable());
        assertTrue(bucket.canPass());
        config.setEnable(false);
        bucket.reInit(config);
        assertFalse(bucket.isEnable());
    }

    @Test
    void startupReadsEnabledAndDisabledConfigurations() {
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(ConfigurationKeys.RATE_LIMIT_ENABLE)).thenReturn(true);
        when(config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND))
                .thenReturn("1");
        when(config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM))
                .thenReturn("10");
        when(config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM))
                .thenReturn("0");
        try (MockedStatic<ConfigurationFactory> factory = mockStatic(ConfigurationFactory.class)) {
            factory.when(ConfigurationFactory::getInstance).thenReturn(config);
            TokenBucketLimiter limiter = new TokenBucketLimiter();
            limiter.init();
            assertTrue(limiter.isEnable());
            assertEquals(10, limiter.obtainConfig().getBucketTokenMaxNum());
            assertTrue(limiter.canPass());
            when(config.getBoolean(ConfigurationKeys.RATE_LIMIT_ENABLE)).thenReturn(false);
            limiter.init();
            assertFalse(limiter.isEnable());
        }
    }

    private void change(RateLimiterHandler handler, String key, String value) {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId(key);
        event.setNewValue(value);
        handler.onChangeEvent(event);
    }
}
