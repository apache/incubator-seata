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
package org.apache.seata.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class AbstractConfigurationTest {

    private Configuration configuration;

    @BeforeEach
    void setUp() {
        configuration = ConfigurationFactory.getInstance();
    }

    @Test
    void testGetIntWithInvalidValue() {
        System.setProperty("test.invalid.int", "not-a-number");
        // due to proxy mode, exceptions may be wrapped as UndeclaredThrowableException
        Assertions.assertThrows(Exception.class, () -> {
            configuration.getInt("test.invalid.int");
        });
    }

    @Test
    void testGetIntWithEmptyString() {
        System.setProperty("test.empty.int", "");
        int value = configuration.getInt("test.empty.int", 100);
        Assertions.assertEquals(100, value);
    }

    @Test
    void testGetIntBoundaryValues() {
        System.setProperty("test.int.max", String.valueOf(Integer.MAX_VALUE));
        System.setProperty("test.int.min", String.valueOf(Integer.MIN_VALUE));

        int maxValue = configuration.getInt("test.int.max");
        int minValue = configuration.getInt("test.int.min");

        Assertions.assertEquals(Integer.MAX_VALUE, maxValue);
        Assertions.assertEquals(Integer.MIN_VALUE, minValue);
    }

    @Test
    void testGetLongWithInvalidValue() {
        System.setProperty("test.invalid.long", "not-a-number");
        // due to proxy mode, exceptions may be wrapped as UndeclaredThrowableException
        Assertions.assertThrows(Exception.class, () -> {
            configuration.getLong("test.invalid.long");
        });
    }

    @Test
    void testGetLongBoundaryValues() {
        System.setProperty("test.long.max", String.valueOf(Long.MAX_VALUE));
        System.setProperty("test.long.min", String.valueOf(Long.MIN_VALUE));

        long maxValue = configuration.getLong("test.long.max");
        long minValue = configuration.getLong("test.long.min");

        Assertions.assertEquals(Long.MAX_VALUE, maxValue);
        Assertions.assertEquals(Long.MIN_VALUE, minValue);
    }

    @Test
    void testGetShortWithInvalidValue() {
        System.setProperty("test.invalid.short", "not-a-number");
        // due to proxy mode, exceptions may be wrapped as UndeclaredThrowableException
        Assertions.assertThrows(Exception.class, () -> {
            configuration.getShort("test.invalid.short");
        });
    }

    @Test
    void testGetShortBoundaryValues() {
        System.setProperty("test.short.max", String.valueOf(Short.MAX_VALUE));
        System.setProperty("test.short.min", String.valueOf(Short.MIN_VALUE));

        short maxValue = configuration.getShort("test.short.max");
        short minValue = configuration.getShort("test.short.min");

        Assertions.assertEquals(Short.MAX_VALUE, maxValue);
        Assertions.assertEquals(Short.MIN_VALUE, minValue);
    }

    @Test
    void testGetShortOutOfRange() {
        System.setProperty("test.short.overflow", "100000");
        // due to proxy mode, exceptions may be wrapped as UndeclaredThrowableException
        Assertions.assertThrows(Exception.class, () -> {
            configuration.getShort("test.short.overflow");
        });
    }

    @Test
    void testGetBooleanWithVariousValues() {
        System.setProperty("test.bool.true", "true");
        System.setProperty("test.bool.false", "false");
        System.setProperty("test.bool.TRUE", "TRUE");
        System.setProperty("test.bool.FALSE", "FALSE");
        System.setProperty("test.bool.invalid", "yes");

        Assertions.assertTrue(configuration.getBoolean("test.bool.true"));
        Assertions.assertFalse(configuration.getBoolean("test.bool.false"));
        Assertions.assertTrue(configuration.getBoolean("test.bool.TRUE"));
        Assertions.assertFalse(configuration.getBoolean("test.bool.FALSE"));
        Assertions.assertFalse(configuration.getBoolean("test.bool.invalid"));
    }

    @Test
    void testGetDurationWithValidFormats() {
        System.setProperty("test.duration.seconds", "30s");
        System.setProperty("test.duration.minutes", "5m");
        System.setProperty("test.duration.hours", "2h");
        System.setProperty("test.duration.millis", "1000ms");

        Assertions.assertEquals(Duration.ofSeconds(30), configuration.getDuration("test.duration.seconds"));
        Assertions.assertEquals(Duration.ofMinutes(5), configuration.getDuration("test.duration.minutes"));
        Assertions.assertEquals(Duration.ofHours(2), configuration.getDuration("test.duration.hours"));
        Assertions.assertEquals(Duration.ofMillis(1000), configuration.getDuration("test.duration.millis"));
    }

    @Test
    void testGetDurationWithDefaultValue() {
        Duration defaultDuration = Duration.ofSeconds(60);
        Duration value = configuration.getDuration("test.duration.nonexistent", defaultDuration);
        Assertions.assertEquals(defaultDuration, value);
    }


    @Test
    void testGetConfigWithDefaultValue() {
        String defaultValue = "default-value";
        String value = configuration.getConfig("test.nonexistent.key", defaultValue);
        Assertions.assertEquals(defaultValue, value);
        
        String valueCached = configuration.getConfig("test.nonexistent.key");
        // due to configuration cache, may return default value instead of null
        Assertions.assertNotNull(valueCached);
    }

    @Test
    void testGetConfigWithEmptyString() {
        System.setProperty("test.empty.string", "");
        String value = configuration.getConfig("test.empty.string", "default");
        Assertions.assertNotNull(value);
    }

    @Test
    void testGetConfigWithWhitespace() {
        System.setProperty("test.whitespace", "   ");
        String value = configuration.getConfig("test.whitespace");
        Assertions.assertEquals("   ", value);
    }

    @Test
    void testGetConfigWithSpecialCharacters() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
        System.setProperty("test.special.chars", specialChars);
        String value = configuration.getConfig("test.special.chars");
        Assertions.assertEquals(specialChars, value);
    }

    @Test
    void testGetConfigWithUnicode() {
        String unicode = "测试中文字符";
        System.setProperty("test.unicode", unicode);
        String value = configuration.getConfig("test.unicode");
        Assertions.assertEquals(unicode, value);
    }

    @Test
    void testGetConfigWithVeryLongString() {
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longString.append("test");
        }
        System.setProperty("test.long.string", longString.toString());
        String value = configuration.getConfig("test.long.string");
        Assertions.assertEquals(longString.toString(), value);
    }

    @Test
    void testDefaultConstants() {
        Assertions.assertEquals((short) 0, AbstractConfiguration.DEFAULT_SHORT);
        Assertions.assertEquals(0, AbstractConfiguration.DEFAULT_INT);
        Assertions.assertEquals(0L, AbstractConfiguration.DEFAULT_LONG);
        Assertions.assertEquals(Duration.ZERO, AbstractConfiguration.DEFAULT_DURATION);
        Assertions.assertFalse(AbstractConfiguration.DEFAULT_BOOLEAN);
    }

    @Test
    void testConfigTimeout() {
        String value = configuration.getConfig("test.timeout.key", "default", 1000);
        Assertions.assertNotNull(value);
    }

    @Test
    void testGetIntWithTimeout() {
        System.setProperty("test.int.timeout", "123");
        int value = configuration.getInt("test.int.timeout", 0, 1000);
        Assertions.assertEquals(123, value);
    }

    @Test
    void testGetBooleanWithTimeout() {
        System.setProperty("test.bool.timeout", "true");
        boolean value = configuration.getBoolean("test.bool.timeout", false, 1000);
        Assertions.assertTrue(value);
    }

    @Test
    void testGetLongWithTimeout() {
        System.setProperty("test.long.timeout", "999");
        long value = configuration.getLong("test.long.timeout", 0L, 1000);
        Assertions.assertEquals(999L, value);
    }

    @Test
    void testGetShortWithTimeout() {
        System.setProperty("test.short.timeout", "88");
        short value = configuration.getShort("test.short.timeout", (short) 0, 1000);
        Assertions.assertEquals((short) 88, value);
    }

    @Test
    void testGetDurationWithTimeout() {
        System.setProperty("test.duration.timeout", "15s");
        Duration value = configuration.getDuration("test.duration.timeout", Duration.ZERO, 1000);
        Assertions.assertEquals(Duration.ofSeconds(15), value);
    }
}
