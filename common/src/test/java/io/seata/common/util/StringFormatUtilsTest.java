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
package io.seata.common.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StringFormatUtilsTest {

    @Test
    void camelToUnderline() {
        assertThat(StringFormatUtils.camelToUnderline(null)).isEqualTo("");
        assertThat(StringFormatUtils.camelToUnderline("  ")).isEqualTo("");
        assertThat(StringFormatUtils.camelToUnderline("abcDefGh")).isEqualTo("abc_def_gh");
    }

    @Test
    void underlineToCamel() {
        assertThat(StringFormatUtils.underlineToCamel(null)).isEqualTo("");
        assertThat(StringFormatUtils.underlineToCamel("  ")).isEqualTo("");
        assertThat(StringFormatUtils.underlineToCamel("abc_def_gh")).isEqualTo("abcDefGh");
    }

    @Test
    void minusToCamel() {
        assertThat(StringFormatUtils.minusToCamel(null)).isEqualTo("");
        assertThat(StringFormatUtils.minusToCamel("  ")).isEqualTo("");
        assertThat(StringFormatUtils.minusToCamel("abc-def-gh")).isEqualTo("abcDefGh");
    }

    @Test
    void dotToCamel() {
        assertThat(StringFormatUtils.dotToCamel(null)).isEqualTo("");
        assertThat(StringFormatUtils.dotToCamel("  ")).isEqualTo("");
        assertThat(StringFormatUtils.dotToCamel("abc.def.gh")).isEqualTo("abcDefGh");
    }
}