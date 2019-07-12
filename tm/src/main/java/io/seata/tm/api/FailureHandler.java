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
package io.seata.tm.api;

/**
 * Callback on failure.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public interface FailureHandler {

    /**
     * On begin failure.
     *
     * @param tx    the tx
     * @param cause the cause
     */
    void onBeginFailure(GlobalTransaction tx, Throwable cause);

    /**
     * On commit failure.
     *
     * @param tx    the tx
     * @param cause the cause
     */
    void onCommitFailure(GlobalTransaction tx, Throwable cause);

    /**
     * On rollback failure.
     *
     * @param tx    the tx
     * @param cause the cause
     */
    void onRollbackFailure(GlobalTransaction tx, Throwable cause);
}
