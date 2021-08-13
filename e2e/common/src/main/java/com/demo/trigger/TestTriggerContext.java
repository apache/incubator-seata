package com.demo.trigger;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class TestTriggerContext implements TestTemplateInvocationContext {

    private final Class<? extends Throwable> throwable;
    private final int invocation;
    private final int times;

    @Override
    public List<Extension> getAdditionalExtensions() {
        return Collections.singletonList(new OneTestExtension(throwable, invocation, times));
    }

    @Override
    public String getDisplayName(final int invocationIndex) {
        return String.format("test attempt #%d", invocationIndex);
    }
}
