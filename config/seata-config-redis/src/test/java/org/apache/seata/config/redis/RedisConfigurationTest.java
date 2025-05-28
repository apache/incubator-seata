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
package org.apache.seata.config.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisConfigurationTest {

    private static final String TEST_DATA_ID = "testDataId";
    private static final String TEST_CONTENT = "testContent";
    private static final String TEST_DEFAULT_VALUE = "testDefaultValue";
    private static final String CONFIGKEY = "seata:config:SEATA_GROUP";

    private static final long TIMEOUT_MILLIS = 5000;

    private static Configuration configuration;
    private static JedisPool jedisPool;

    private final List<Logger> watchedLoggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    @BeforeAll
    public static void setUp() {
        System.setProperty("config.type", "redis");
        System.setProperty("config.redis.server-addr", "127.0.0.1:6379");

        configuration = RedisConfiguration.getInstance();
        jedisPool = new JedisPool("127.0.0.1", 6379);

        // clean up all test data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CONFIGKEY);
        }
    }

    @AfterAll
    public static void tearDown() {
        // clean up all test data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CONFIGKEY);
        }
        jedisPool.close();
    }

    @BeforeEach
    public void startLogWatcher() {
        logWatcher.start();

        Logger logger = ((Logger) LoggerFactory.getLogger(RedisConfiguration.class.getName()));
        logger.addAppender(logWatcher);

        watchedLoggers.add(logger);
    }

    @AfterEach
    public void cleanLogWatcher() {
        watchedLoggers.forEach(Logger::detachAndStopAllAppenders);
    }

    @Test
    public void testGetLatestConfig() throws InterruptedException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(CONFIGKEY, TEST_DATA_ID, TEST_CONTENT);
        }

        // If there is no cache, query redis
        String result = configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS);
        assertEquals(TEST_CONTENT, result);

        // If there is a cache
        configuration.putConfig(TEST_DATA_ID, "newTestContent");
        assertEquals("newTestContent", configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS));

        cleanupAfterTest(TEST_DATA_ID, null);
    }

    @Test
    public void testPutConfig() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch);

        assertTrue(configuration.putConfig(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(TEST_CONTENT, configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS));

        cleanupAfterTest(TEST_DATA_ID, listener);
    }

    @Test
    public void testRemoveConfig() throws InterruptedException {
        configuration.putConfig(TEST_DATA_ID, TEST_CONTENT);

        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, null, latch);

        assertTrue(configuration.removeConfig(TEST_DATA_ID, TIMEOUT_MILLIS));
        assertNull(configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS));
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        cleanupAfterTest(TEST_DATA_ID, listener);
    }

    @Test
    public void testPutConfigIfAbsent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch);

        // 1.When seataConfig and redis is empty, put config to redis
        boolean success = configuration.putConfigIfAbsent(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS);
        assertTrue(success);
        assertEquals(TEST_CONTENT, configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS));
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // 2.When local cache is available, return true directly
        boolean secondTry = configuration.putConfigIfAbsent(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS);
        assertTrue(secondTry);

        // 3.When seataConfig is empty and redis is not empty, update the value of redis to seataConfig
        String newDataId = "newTestDataId";
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(CONFIGKEY, newDataId, "alreadyExist");
        }

        boolean thirdTry = configuration.putConfigIfAbsent(newDataId, "newTestContent", TIMEOUT_MILLIS);
        assertTrue(thirdTry);
        assertEquals("alreadyExist", configuration.getLatestConfig(newDataId, null, TIMEOUT_MILLIS));

        cleanupAfterTest(TEST_DATA_ID, listener);
        cleanupAfterTest(newDataId, null);
    }

    @Test
    public void testGetConfigListeners() throws InterruptedException {
        Set<ConfigurationChangeListener> initialListeners = configuration.getConfigListeners(TEST_DATA_ID);
        assertNull(initialListeners);

        ConfigurationChangeListener listener1 = addPublishListener(TEST_DATA_ID, TEST_CONTENT, new CountDownLatch(1));
        ConfigurationChangeListener listener2 = addPublishListener(TEST_DATA_ID, TEST_CONTENT, new CountDownLatch(1));

        Set<ConfigurationChangeListener> listeners = configuration.getConfigListeners(TEST_DATA_ID);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1) && listeners.contains(listener2));

        configuration.removeConfigListener(TEST_DATA_ID, listener1);
        listeners = configuration.getConfigListeners(TEST_DATA_ID);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener2));

        Set<ConfigurationChangeListener> nonExistentListeners = configuration.getConfigListeners("nonExistentDataId");
        assertNull(nonExistentListeners);
    }

    @Test
    public void testAllConfigMethodsWithRedisException() throws Exception {
        JedisPool mockJedisPool = mock(JedisPool.class);

        // let mockJedisPool throws a runtime exception when calling getResource()
        RuntimeException simulatedException = new RuntimeException("Simulated Redis connection failure");
        when(mockJedisPool.getResource()).thenThrow(simulatedException);

        // Replace the static jedisPool field inside the RedisConfiguration with a mock object by reflection
        Field jedisPoolField = RedisConfiguration.class.getDeclaredField("jedisPool");
        jedisPoolField.setAccessible(true);
        jedisPoolField.set(null, mockJedisPool);

        // 1.test getLatestConfig
        assertEquals(TEST_DEFAULT_VALUE, configuration.getLatestConfig(TEST_DATA_ID, TEST_DEFAULT_VALUE, TIMEOUT_MILLIS));
        assertEquals("Failed to get config from Redis: Simulated Redis connection failure", getLogs(Level.ERROR).get(0));

        // 2.test putConfig
        assertFalse(configuration.putConfig(TEST_DATA_ID, TEST_DEFAULT_VALUE, TIMEOUT_MILLIS));
        assertEquals("Failed to put config to Redis: Simulated Redis connection failure", getLogs(Level.ERROR).get(1));

        // 3.test removeConfig
        assertFalse(configuration.removeConfig(TEST_DATA_ID, TIMEOUT_MILLIS));
        assertEquals("Failed to remove config from Redis: Simulated Redis connection failure", getLogs(Level.ERROR).get(2));

        // 4.test putConfigIfAbsent
        clearSeataConfigCache();
        assertFalse(configuration.putConfigIfAbsent(TEST_DATA_ID, TEST_DEFAULT_VALUE, TIMEOUT_MILLIS));
        assertEquals("Failed to put config if absent to Redis: Simulated Redis connection failure", getLogs(Level.ERROR).get(3));

        // 5.test addConfigListener
        addPublishListener(TEST_DATA_ID, TEST_DEFAULT_VALUE, new CountDownLatch(1));
        assertEquals("Failed to subscribe Redis channel: Simulated Redis connection failure", getLogs(Level.ERROR).get(4));

        verify(mockJedisPool, times(5)).getResource();

        // Restore the jedisPool to its original value to ensure that other tests are not affected
        jedisPoolField.set(null, new JedisPool("127.0.0.1", 6379));
    }

    @Test
    public void testGetTypeName() {
        RedisConfiguration redisConfiguration = RedisConfiguration.getInstance();
        assertEquals(redisConfiguration.getTypeName(), "redis");
    }

    @Test
    public void testGetInstance() {
        assertInstanceOf(RedisConfiguration.class, RedisConfiguration.getInstance());
        assertEquals(configuration, RedisConfiguration.getInstance());
    }

    /**
     * Clear the cache and make sure that the putConfigIfAbsent goes to the 'try (Jedis jedis = jedisPool.getResource())' branch
     */
    private void clearSeataConfigCache() {
        try {
            Field seataConfigField = RedisConfiguration.class.getDeclaredField("seataConfig");
            seataConfigField.setAccessible(true);
            Properties seataConfig = (Properties) seataConfigField.get(null);
            if (seataConfig != null) {
                seataConfig.clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear seataConfig cache", e);
        }
    }

    /**
     * Create and add a configuration listener, check if publish notifications are successful
     */
    private ConfigurationChangeListener addPublishListener(String dataId, String expectedContent, CountDownLatch latch)
            throws InterruptedException {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
            }

            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                if (event.getDataId().equals(dataId)) {
                    String newValue = event.getNewValue();
                    if ((expectedContent == null && newValue == null)
                            || (expectedContent != null && expectedContent.equals(newValue))) {
                        latch.countDown();
                    }
                }
            }
        };

        configuration.addConfigListener(dataId, listener);

        // add sleep to ensure listener added successfully
        Thread.sleep(100);

        return listener;
    }

    /**
     * Clean up resources after each test
     */
    private void cleanupAfterTest(String dataId, ConfigurationChangeListener listener) throws InterruptedException {
        // remove the listener
        if (listener != null) {
            configuration.removeConfigListener(dataId, listener);
        }

        // clean redis data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(CONFIGKEY, dataId);
        }

        // clean up the cache in seataConfig
        try {
            Field seataConfigField = RedisConfiguration.class.getDeclaredField("seataConfig");
            seataConfigField.setAccessible(true);
            Properties seataConfig = (Properties) seataConfigField.get(null);
            seataConfig.remove(dataId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean seataConfig cache", e);
        }

        // add sleep to ensure the security of test data
        Thread.sleep(100);
    }

    private List<String> getLogs(Level level) {
        return logWatcher.list.stream()
                .filter(event -> event.getLoggerName().endsWith(RedisConfiguration.class.getName())
                        && event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
