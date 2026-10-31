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

/**
 * Structured outcome of a signature verification. Using a rich result object instead of a
 * boolean lets the caller build accurate audit logs / metrics without re-running the check.
 */
public final class VerificationResult {

    private static final VerificationResult OK = new VerificationResult(true, null, null);

    private final boolean success;

    private final SecurityConstants.ErrorCode errorCode;

    private final String message;

    private VerificationResult(boolean success, SecurityConstants.ErrorCode errorCode, String message) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static VerificationResult ok() {
        return OK;
    }

    public static VerificationResult failure(SecurityConstants.ErrorCode code, String message) {
        return new VerificationResult(false, code, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public SecurityConstants.ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
