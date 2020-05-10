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
package io.seata.tm.api.transaction;

/**
 * @author guoyao
 */
public interface TransactionHook {

    /**
     * before tx begin
     */
    void beforeBegin();

    /**
     * after tx begin
     */
    void afterBegin();

    /**
     * before tx commit
     */
    void beforeCommit();

    /**
     * after tx commit
     */
    void afterCommit();

    /**
     * before tx rollback
     */
    void beforeRollback();

    /**
     * after tx rollback
     */
    void afterRollback();

    /**
     * after tx all Completed
     */
    void afterCompletion();
}
