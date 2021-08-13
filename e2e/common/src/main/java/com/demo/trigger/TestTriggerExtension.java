package com.demo.trigger;

import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class TestTriggerExtension implements TestTemplateInvocationContextProvider {

    public static final String TEST_SHOULD_RETRY = "TEST_PASSED";

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return context.getTestMethod().map(m -> m.isAnnotationPresent(TestTrigger.class)).orElse(false);
    }

    /**
     * Provide test context for methods annotated with TestTrigger
     * @param context
     * @return
     */
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
        final TestTrigger TestTrigger = context.getRequiredTestMethod().getAnnotation(TestTrigger.class);
        final Class<? extends Throwable> throwable = TestTrigger.throwable();
        final long interval = TestTrigger.interval();
        final int times = TestTrigger.value();

        if (interval <= 0) {
            throw new IllegalArgumentException(
                    "TestTrigger#interval must be a positive integer, but was " + interval
            );
        }

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(new Iterator<Integer>() {
                    final AtomicInteger count = new AtomicInteger(0);

                    @Override
                    @SneakyThrows
                    // Main logical judgment of whether to return the TestTriggerContext
                    public boolean hasNext() {

                        final boolean shouldRetry = StoresUtil.get(context, TEST_SHOULD_RETRY, Boolean.class) != Boolean.FALSE;

                        final boolean hasNext = (times < 0 || count.get() <= times) && shouldRetry;
                        // Interval between any two retries
                        if (hasNext) {
                            Thread.sleep(interval);
                        }

                        return hasNext;
                    }

                    @Override
                    public Integer next() {
                        return count.getAndIncrement();
                    }
                }, Spliterator.NONNULL), false
        ).map(time -> new TestTriggerContext(throwable, time, times));
    }
}
