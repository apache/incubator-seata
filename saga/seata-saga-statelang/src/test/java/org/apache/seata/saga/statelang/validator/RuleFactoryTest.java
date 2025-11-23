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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RuleFactoryTest {

    @Test
    public void testGetRules() {
        List<Rule> rules = RuleFactory.getRules();
        Assertions.assertNotNull(rules);
        Assertions.assertFalse(rules.isEmpty(), "Rule list should not be empty");

        for (Rule rule : rules) {
            Assertions.assertNotNull(rule.getName(), "Rule name should not be null");
            Assertions.assertFalse(rule.getName().trim().isEmpty(), "Rule name should not be empty");
        }
    }

    @Test
    public void testRuleFactorySingleton() {
        List<Rule> rules1 = RuleFactory.getRules();
        List<Rule> rules2 = RuleFactory.getRules();
        Assertions.assertSame(rules1, rules2, "Rule list should be singleton");
    }
}
