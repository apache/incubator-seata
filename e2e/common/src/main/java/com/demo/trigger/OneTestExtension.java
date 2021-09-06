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
// This file is originally from Apache SkyWalking
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
 *
 */

package com.demo.trigger;

import org.junit.jupiter.api.extension.*;
import org.opentest4j.TestAbortedException;

import java.util.Optional;

import static com.demo.trigger.TestTriggerExtension.TEST_SHOULD_RETRY;

public class OneTestExtension implements ExecutionCondition, AfterTestExecutionCallback, TestExecutionExceptionHandler {

    // When not specified, the default initialization is Throwable.class
    private final Class<? extends Throwable>[] throwables;
    private final int invocation;
    private final int times;

    public OneTestExtension(Class<? extends Throwable>[] throwables, int invocation, int times) {
        this.throwables = throwables;
        this.invocation = invocation;
        this.times = times;
    }

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
            throw new TestAbortedException("Test failed, will retry", throwable);
        }

        for (Class<? extends Throwable> throwableClass : throwables) {
            if (throwableClass == throwable.getClass()) {
                throw new TestAbortedException("Test failed, will retry", throwable);
            }
        }

        throw throwable;
    }

    private boolean retryOnAnyException() {
        return throwables.length == 0;
    }

    private boolean retryInfinitely() {
        return times < 0;
    }
}