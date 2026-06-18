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
package org.apache.seata.saga.engine.expression.spel;

import org.apache.seata.saga.engine.expression.Expression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.util.HashMap;
import java.util.Map;

public class SpringELExpressionFactoryTest {
    @Test
    public void testCreateExpression() {
        SpringELExpressionFactory factory = new SpringELExpressionFactory(null);
        Assertions.assertNotNull(factory.createExpression("'Hello World'.concat('!')"));
    }

    @Test
    public void testPropertyAccessWorks() {
        SpringELExpressionFactory factory = new SpringELExpressionFactory(null);
        Expression expression = factory.createExpression("[\"name\"]");
        Map<String, Object> context = new HashMap<>();
        context.put("name", "seata");
        Object value = expression.getValue(context);
        Assertions.assertEquals("seata", value);
    }

    @Test
    public void testTypeAccessBlocked_Runtime() {
        SpringELExpressionFactory factory = new SpringELExpressionFactory(null);
        Expression expression = factory.createExpression("T(Runtime).getRuntime().exec('whoami')");
        Assertions.assertThrows(SpelEvaluationException.class, () -> expression.getValue(new HashMap<>()));
    }

    @Test
    public void testTypeAccessBlocked_System() {
        SpringELExpressionFactory factory = new SpringELExpressionFactory(null);
        Expression expression = factory.createExpression("T(System).getProperty('user.name')");
        Assertions.assertThrows(SpelEvaluationException.class, () -> expression.getValue(new HashMap<>()));
    }

    @Test
    public void testTypeAccessBlocked_ProcessBuilder() {
        SpringELExpressionFactory factory = new SpringELExpressionFactory(null);
        Expression expression = factory.createExpression("T(java.lang.ProcessBuilder).new({'whoami'}).start()");
        Assertions.assertThrows(SpelEvaluationException.class, () -> expression.getValue(new HashMap<>()));
    }
}
