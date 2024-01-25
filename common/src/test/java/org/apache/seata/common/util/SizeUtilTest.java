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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SizeUtilTest {
    @Test
    void size2Long() {
        assertThatThrownBy(() -> SizeUtil.size2Long(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SizeUtil.size2Long("")).isInstanceOf(IllegalArgumentException.class);
        // wrong format
        assertThatThrownBy(() -> SizeUtil.size2Long("2kk")).isInstanceOf(IllegalArgumentException.class);
        // wrong unit
        assertThatThrownBy(() -> SizeUtil.size2Long("2x")).isInstanceOf(IllegalArgumentException.class);

        assertThat(SizeUtil.size2Long("2k")).isEqualTo(2L * 1024);
        assertThat(SizeUtil.size2Long("2m")).isEqualTo(2L * 1024 * 1024);
        assertThat(SizeUtil.size2Long("2G")).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(SizeUtil.size2Long("2t")).isEqualTo(2L * 1024 * 1024 * 1024 * 1024);
    }
}
