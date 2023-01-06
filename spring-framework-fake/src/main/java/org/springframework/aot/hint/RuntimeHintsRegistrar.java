package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

@FunctionalInterface
public interface RuntimeHintsRegistrar {

    void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);

}
