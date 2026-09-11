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
package io.seata.config;

import io.seata.common.util.DurationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * @author jsbxyyx
 */
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
    }

}
