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
package io.seata.mockserver;


public enum ExpectTransactionResult {

    AllCommitted(0, "all success"),
    AllRollbacked(1, "all rollback"),
    PhaseOneTimeoutRollbacked(2, "phase one failed");

    private final int code;
    private final String desc;

    ExpectTransactionResult(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    public static ExpectTransactionResult covert(int code) {
        for (ExpectTransactionResult result : ExpectTransactionResult.values()) {
            if (result.getCode() == code) {
                return result;
            }
        }
        return null;
    }
}
