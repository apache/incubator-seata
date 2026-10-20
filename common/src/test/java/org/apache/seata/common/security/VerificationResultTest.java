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
package org.apache.seata.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationResultTest {

    @Test
    void ok_returns_singleton() {
        assertSame(
                VerificationResult.ok(),
                VerificationResult.ok(),
                "success result should be shared to avoid allocation");
        assertTrue(VerificationResult.ok().isSuccess());
        assertNull(VerificationResult.ok().getErrorCode());
        assertNull(VerificationResult.ok().getMessage());
    }

    @Test
    void failure_captures_code_and_message() {
        VerificationResult r = VerificationResult.failure(SecurityConstants.ErrorCode.BAD_SIGNATURE, "boom");
        assertFalse(r.isSuccess());
        assertEquals(SecurityConstants.ErrorCode.BAD_SIGNATURE, r.getErrorCode());
        assertEquals("boom", r.getMessage());
    }

    @Test
    void failure_allows_null_message() {
        VerificationResult r = VerificationResult.failure(SecurityConstants.ErrorCode.FORBIDDEN, null);
        assertFalse(r.isSuccess());
        assertNull(r.getMessage());
    }
}
