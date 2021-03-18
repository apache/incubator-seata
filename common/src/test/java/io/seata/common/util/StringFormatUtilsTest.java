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