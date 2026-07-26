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
package org.apache.seata.namingserver.security;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAutoConfigurationTest {

    @Test
    void glob_to_regex_translates_star() {
        Pattern p = SecurityAutoConfiguration.globToRegex("tenant_a_*");
        assertTrue(p.matcher("tenant_a_group").matches());
        assertTrue(p.matcher("tenant_a_").matches());
        assertFalse(p.matcher("tenant_b_group").matches());
    }

    @Test
    void glob_to_regex_translates_question_mark() {
        Pattern p = SecurityAutoConfiguration.globToRegex("g?");
        assertTrue(p.matcher("g1").matches());
        assertFalse(p.matcher("g12").matches());
    }

    @Test
    void glob_to_regex_escapes_regex_special_chars() {
        // A literal '.' in a glob must not match arbitrary characters.
        Pattern p = SecurityAutoConfiguration.globToRegex("group.a");
        assertTrue(p.matcher("group.a").matches());
        assertFalse(p.matcher("groupXa").matches(),
                "'.' must be treated literally, not as regex any-char");
    }

    @Test
    void glob_to_regex_supports_leading_wildcard() {
        Pattern p = SecurityAutoConfiguration.globToRegex("*_prod");
        assertTrue(p.matcher("a_prod").matches());
        assertTrue(p.matcher("_prod").matches());
        assertFalse(p.matcher("prod").matches());
    }
}
