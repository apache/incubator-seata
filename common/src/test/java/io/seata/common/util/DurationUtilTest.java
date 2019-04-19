/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package io.seata.common.util;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author XCXCXCXCX
 */
public class DurationUtilTest {

    @Test
    public void parseTest(){
        assertThat(DurationUtil.parse("1d")).isEqualTo(Duration.ofDays(1));
        assertThat(DurationUtil.parse("1h")).isEqualTo(Duration.ofHours(1));
        assertThat(DurationUtil.parse("1m")).isEqualTo(Duration.ofMinutes(1));
        assertThat(DurationUtil.parse("1s")).isEqualTo(Duration.ofSeconds(1));
        assertThat(DurationUtil.parse("1ms")).isEqualTo(Duration.ofMillis(1));
        assertThat(DurationUtil.parse("-1ms")).isEqualTo(Duration.ofMillis(-1));
        assertThat(DurationUtil.parse("-1ms").getSeconds()).isEqualTo(-1L);
    }
}
