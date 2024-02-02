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

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.DurationUtil;
import org.apache.seata.common.util.ReflectionUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;


public class ConfigurationCacheTests {

    @Test
    public void testChangeValue() throws Exception {
        Configuration configuration = new FileConfiguration("registry");
        configuration = ConfigurationCache.getInstance().proxy(configuration);
        configuration.getBoolean("aaa", false);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("aaa", "true"));
        boolean aaa = configuration.getBoolean("aaa", false);
        Assertions.assertTrue(aaa);

        configuration.getShort("bbb", (short) 0);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("bbb", "1"));
        short bbb = configuration.getShort("bbb", (short) 0);
        Assertions.assertEquals((short) 1, bbb);

        configuration.getDuration("ccc", Duration.ZERO);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("ccc", "1s"));
        Duration ccc = configuration.getDuration("ccc", Duration.ZERO);
        Assertions.assertEquals(DurationUtil.parse("1s"), ccc);

        configuration.getInt("ddd", 0);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("ddd", "1"));
        int ddd = configuration.getInt("ddd", 0);
        Assertions.assertEquals(1, ddd);

        configuration.getLong("eee", 0);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("eee", "1"));
        long eee = configuration.getLong("eee", 0);
        Assertions.assertEquals(1, eee);

        // test null
        configuration.getConfig("test", null);
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("test", "1"));
        String test = configuration.getConfig("test", null);
        Assertions.assertEquals("1", test);
        // new value is null
        ConfigurationCache.getInstance().onChangeEvent(new ConfigurationChangeEvent("test", null));
        test = configuration.getConfig("test", null);
        Assertions.assertNull(test);
    }

    // FIXME: 2023/2/19 wait bugfix
    // @Test
    public void testConfigListener() throws Exception {
        Configuration configuration = new FileConfiguration("registry");
        configuration = ConfigurationCache.getInstance().proxy(configuration);

        // get config listeners map
        Field configListenersMapField = ReflectionUtil.getField(ConfigurationCache.class, "configListenersMap");
        Map<String, HashSet<ConfigurationChangeListener>> configListenersMap = (Map<String,
            HashSet<ConfigurationChangeListener>>)configListenersMapField.get(ConfigurationCache.getInstance());

        boolean value = configuration.getBoolean("service.disableGlobalTransaction");
        TestListener listener = new TestListener();
        ConfigurationCache.addConfigListener("service.disableGlobalTransaction", listener);
        // check listener if exist
        HashSet<ConfigurationChangeListener> listeners = configListenersMap.get("service.disableGlobalTransaction");
        Assertions.assertTrue(CollectionUtils.isNotEmpty(listeners));
        // change value,trigger listener
        System.setProperty("service.disableGlobalTransaction", String.valueOf(!value));
        // remove null
        ConfigurationCache.removeConfigListener(null);
        // check listener if exist
        listeners = configListenersMap.get("service.disableGlobalTransaction");
        Assertions.assertTrue(CollectionUtils.isNotEmpty(listeners));
        // remove listener
        ConfigurationCache.removeConfigListener("service.disableGlobalTransaction", listener);
        // check listener if exist
        listeners = configListenersMap.get("service.disableGlobalTransaction");
        // is empty
        Assertions.assertTrue(CollectionUtils.isEmpty(listeners));
    }

    public static class TestListener implements ConfigurationChangeListener {

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            Assertions.assertEquals(Boolean.parseBoolean(event.getNewValue()),
                !Boolean.parseBoolean(event.getOldValue()));
        }
    }

}
