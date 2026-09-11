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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wang.liang
 */
class ConfigurationTest {

    private static final String NULL_POSTFIX = "_null";
    private static final String DEFAULT_POSTFIX = "_default";

    private static final String STRING_VALUE = "aaaa";
    private static final short SHORT_VALUE = (short)1;
    private static final int INT_VALUE = 2;
    private static final long LONG_VALUE = 3L;
    private static final Duration DURATION_VALUE = Duration.ofSeconds(4);
    private static final boolean BOOLEAN_VALUE = true;

    private static final String DEFAULT_STRING_VALUE = "BBBB";
    private static final short DEFAULT_SHORT_VALUE = (short)2;
    private static final int DEFAULT_INT_VALUE = 3;
    private static final long DEFAULT_LONG_VALUE = 4L;
    private static final Duration DEFAULT_DURATION_VALUE = Duration.ofSeconds(5);
    private static final boolean DEFAULT_BOOLEAN_VALUE = true;

    @Test
    void test_getConfig_Methods() {
        String dataId;
        Configuration configuration = ConfigurationFactory.getInstance();

        //string
        dataId = "string";
        assertThat(configuration.getConfig(dataId)).isEqualTo(STRING_VALUE);
        assertThat(configuration.getConfig(dataId + NULL_POSTFIX)).isNull();
        assertThat(configuration.getConfig(dataId + DEFAULT_POSTFIX, DEFAULT_STRING_VALUE)).isEqualTo(DEFAULT_STRING_VALUE);

        //short
        dataId = "short";
        assertThat(configuration.getShort(dataId)).isEqualTo(SHORT_VALUE);
        assertThat(configuration.getShort(dataId + NULL_POSTFIX)).isEqualTo(AbstractConfiguration.DEFAULT_SHORT);
        assertThat(configuration.getShort(dataId + DEFAULT_POSTFIX, DEFAULT_SHORT_VALUE)).isEqualTo(DEFAULT_SHORT_VALUE);

        //int
        dataId = "int";
        assertThat(configuration.getInt(dataId)).isEqualTo(INT_VALUE);
        assertThat(configuration.getInt(dataId + NULL_POSTFIX)).isEqualTo(AbstractConfiguration.DEFAULT_INT);
        assertThat(configuration.getInt(dataId + DEFAULT_POSTFIX, DEFAULT_INT_VALUE)).isEqualTo(DEFAULT_INT_VALUE);

        //long
        dataId = "long";
        assertThat(configuration.getLong(dataId)).isEqualTo(LONG_VALUE);
        assertThat(configuration.getLong(dataId + NULL_POSTFIX)).isEqualTo(AbstractConfiguration.DEFAULT_LONG);
        assertThat(configuration.getLong(dataId + DEFAULT_POSTFIX, DEFAULT_LONG_VALUE)).isEqualTo(DEFAULT_LONG_VALUE);

        //duration
        dataId = "duration";
        assertThat(configuration.getDuration(dataId)).isEqualTo(DURATION_VALUE);
        assertThat(configuration.getDuration(dataId + NULL_POSTFIX)).isEqualTo(AbstractConfiguration.DEFAULT_DURATION);
        assertThat(configuration.getDuration(dataId + DEFAULT_POSTFIX, DEFAULT_DURATION_VALUE)).isEqualTo(DEFAULT_DURATION_VALUE);

        //boolean
        dataId = "boolean";
        assertThat(configuration.getBoolean(dataId)).isEqualTo(BOOLEAN_VALUE);
        assertThat(configuration.getBoolean(dataId + NULL_POSTFIX)).isEqualTo(AbstractConfiguration.DEFAULT_BOOLEAN);
        assertThat(configuration.getBoolean(dataId + DEFAULT_POSTFIX, DEFAULT_BOOLEAN_VALUE)).isEqualTo(DEFAULT_BOOLEAN_VALUE);
    }
}