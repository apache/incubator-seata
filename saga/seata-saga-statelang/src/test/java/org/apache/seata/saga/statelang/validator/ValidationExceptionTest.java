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
package org.apache.seata.saga.statelang.validator;

import org.apache.seata.saga.statelang.domain.StateMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidationExceptionTest {

    @Test
    public void testExceptionWithMessage() {
        Rule mockRule = new TestRule("TestRule", "Please check the state machine definition");
        ValidationException exception = new ValidationException(mockRule, "Invalid state configuration");

        Assertions.assertEquals(
                "Rule [TestRule]: Invalid state configuration, hints: Please check the state machine definition",
                exception.getMessage());
    }

    @Test
    public void testExceptionWithCause() {
        Rule mockRule = new TestRule("ErrorRule", null);
        Throwable cause = new IllegalArgumentException("Invalid parameter");
        ValidationException exception = new ValidationException(mockRule, "Processing failed", cause);

        Assertions.assertEquals("Rule [ErrorRule]: Processing failed", exception.getMessage());
        Assertions.assertSame(cause, exception.getCause());
        Assertions.assertEquals("Invalid parameter", exception.getCause().getMessage());
    }

    @Test
    public void testExceptionWithoutHint() {
        Rule mockRule = new TestRule("NoHintRule", null);
        ValidationException exception = new ValidationException(mockRule, "No hint provided");

        Assertions.assertEquals("Rule [NoHintRule]: No hint provided", exception.getMessage());
    }

    private static class TestRule implements Rule {
        private final String name;
        private final String hint;

        public TestRule(String name, String hint) {
            this.name = name;
            this.hint = hint;
        }

        @Override
        public boolean validate(StateMachine stateMachine) {
            return false;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getHint() {
            return hint;
        }
    }
}
