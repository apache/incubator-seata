package com.demo.trigger;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.util.Optional;

import static com.demo.trigger.TestTriggerExtension.TEST_SHOULD_RETRY;

@RequiredArgsConstructor
public class OneTestExtension implements ExecutionCondition, AfterTestExecutionCallback, TestExecutionExceptionHandler {

    // When not specified, the default initialization is Throwable.class
    private final Class<? extends Throwable> throwableClass;
    private final int invocation;
    private final int times;

    @Override

    /**
     * Get the exception occurred in the test method, decide whether to retry next time
     * based on the exception.
     */
    public void afterTestExecution(ExtensionContext context) throws Exception {

        final Optional<Throwable> throwable = context.getExecutionException();
        StoresUtil.put(
                context, TEST_SHOULD_RETRY,
                throwable.isPresent() && throwable.get().getClass() == TestAbortedException.class
        );
    }

    @Override
    // Assess whether the test method needs to be started
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final boolean shouldRetry = StoresUtil.get(context, TEST_SHOULD_RETRY, Boolean.class) != Boolean.FALSE;

        if (!shouldRetry) {
            return ConditionEvaluationResult.disabled("test passed after " + invocation + " attempts");
        }

        StoresUtil.put(context, TEST_SHOULD_RETRY, invocation < times || retryInfinitely());
        return ConditionEvaluationResult.enabled("test is retried " + times + " times and is still failed");

    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {

        if (StoresUtil.get(context, TEST_SHOULD_RETRY, Boolean.class) == Boolean.FALSE) {
            throw throwable;
        }


        if (retryOnAnyException()) {
            throw new TestAbortedException("test failed, will retry", throwable);
        }

        if (throwableClass == throwable.getClass()) {
            throw new TestAbortedException("test failed, will retry", throwable);
        }

        throw throwable;
    }

    private boolean retryOnAnyException() {
        return throwableClass == Throwable.class;
    }

    private boolean retryInfinitely() {
        return times < 0;
    }
}
