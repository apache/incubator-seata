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
package io.seata.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * operation of global transaction.
 *
 * @author leizhiyuan
 */
public enum GlobalOperation {

    /**
     * Start.
     */
    // Unknown
    BEGIN(0),

    /**
     * first Commit.
     */
    COMMIT(1),

    /**
     *
     */
    // ASNYC COMMIT.
    ASYNC_COMMIT(2),

    /**
     * RETRY_COMMIT
     */
    RETRY_COMMIT(3),

    /**
     * END_COMMIT_SUCCESS
     */
    END_COMMIT_SUCCESS(4),

    /**
     * END_COMMIT_FAIL
     */
    END_COMMIT_FAIL(5),

    /**
     * ROLLBACK
     */
    ROLLBACK(6),

    /**
     * RETRY_ROLLBACK_NORMAL
     */
    RETRY_ROLLBACK_NORMAL(7),

    /**
     * END_ROLLBACK_SUCCESS_NORMAL
     */
    END_ROLLBACK_SUCCESS_NORMAL(8),

    /**
     * END_ROLLBACK_FAIL_NORMAL
     */
    END_ROLLBACK_FAIL_NORMAL(9),

    /**
     * END_ROLLBACK_SUCCESS_NORMAL
     */
    END_ROLLBACK_SUCCESS_TIMEOUT(10),

    /**
     * END_ROLLBACK_SUCCESS_NORMAL
     */
    END_ROLLBACK_FAIL_TIMEOUT(11),

    /**
     * RETRY_ROLLBACK_NORMAL
     */
    RETRY_ROLLBACK_TIMEOUT(12),

    /**
     * The TIMEOUT CHECK
     */
    TIMEOUT(13),

    /**
     * The FINSH
     */
    FINISH(14),

    ;

    private int code;

    GlobalOperation(int code) {
        this.code = code;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    private static final Map<Integer, GlobalOperation> MAP = new HashMap<>(values().length);

    static {
        for (GlobalOperation ops : values()) {
            MAP.put(ops.code, ops);
        }
    }

    /**
     * Get global operation.
     *
     * @param code the code
     * @return the global operation
     */
    public static GlobalOperation get(byte code) {
        return get((int)code);
    }

    /**
     * Get global operation.
     *
     * @param code the code
     * @return the global operation
     */
    public static GlobalOperation get(int code) {
        GlobalOperation operation = MAP.get(code);

        if (null == operation) {
            throw new IllegalArgumentException("Unknown GlobalOperation[" + code + "]");
        }

        return operation;
    }
}
