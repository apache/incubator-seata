
package com.demo.utils;

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Envs {
    public static String resolve(final String text) {
        if (Strings.isNullOrEmpty(text)) {
            return "";
        }

        final Pattern pattern = Pattern.compile("\\$\\{(?<name>\\w+)}");
        final Matcher matcher = pattern.matcher(text);
        final StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            final String name = matcher.group("name");
            final String value = System.getenv(name);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(Strings.nullToEmpty(value)));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }
}
