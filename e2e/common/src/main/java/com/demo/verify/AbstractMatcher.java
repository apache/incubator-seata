package com.demo.verify;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Value verification tool class, after extends this class, write specific verification logic.
 */
public abstract class AbstractMatcher<T> {
    // S + is any white space character, <Val> is group name.
    // ne, eq, gt and so on is the required verification operation.
    private static final Pattern NE_MATCHER = Pattern.compile("ne\\s+(?<val>.+)");
    private static final Pattern EQ_MATCHER = Pattern.compile("eq\\s+(?<val>.+)");
    private static final Pattern GT_MATCHER = Pattern.compile("gt\\s+(?<val>.+)");
    private static final Pattern GE_MATCHER = Pattern.compile("ge\\s+(?<val>.+)");
    private static final Pattern NN_MATCHER = Pattern.compile("^not null$");
    private static final Pattern RE_MATCHER = Pattern.compile("^re\\((?<regexp>.+)\\)$");

    /**
     *
     * @param t is the data expected to be verified
     */
    public abstract void verify(T t);


    protected void doVerify(String expected, int actual) {
        this.doVerify(expected, String.valueOf(actual));
    }

    protected void doVerify(String expected, long actual) {
        this.doVerify(expected, String.valueOf(actual));
    }

    protected void doVerify(String expected, boolean actual) {
        this.doVerify(expected, String.valueOf(actual));
    }

    protected void doVerify(String expected, String actual) {
        Matcher matcher = NN_MATCHER.matcher(expected);
        if (matcher.find()) {
            assertThat(actual).isNotNull();
            return;
        }

        matcher = NE_MATCHER.matcher(expected);
        if (matcher.find()) {
            assertThat(actual).isNotEqualTo(matcher.group("val"));
            return;
        }

        matcher = GT_MATCHER.matcher(expected);
        if (matcher.find()) {
            String val = matcher.group("val");

            assertThat(val).isNotBlank();
            assertThat(Double.parseDouble(actual)).isGreaterThan(Double.parseDouble(val));
            return;
        }

        matcher = GE_MATCHER.matcher(expected);
        if (matcher.find()) {
            String val = matcher.group("val");

            assertThat(val).isNotBlank();
            assertThat(Double.parseDouble(actual)).isGreaterThanOrEqualTo(Double.parseDouble(val));
            return;
        }

        matcher = EQ_MATCHER.matcher(expected);
        if (matcher.find()) {
            String val = matcher.group("val");

            assertThat(val).isNotBlank();
            assertThat(Double.parseDouble(actual)).isEqualTo(Double.parseDouble(val));
            return;
        }

        matcher = RE_MATCHER.matcher(expected);
        if (matcher.find()) {
            String regexp = matcher.group("regexp");

            assertThat(regexp).isNotBlank();
            assertThat(actual).matches(regexp);
            return;
        }

        assertThat(actual).isEqualTo(expected);
    }

}
