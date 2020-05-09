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

import io.seata.tm.api.GlobalTransaction;

/**
 * @author guoyao
 */
public class TransactionHookAdapter implements TransactionHook {

    @Override
    public void beforeBegin(GlobalTransaction tx) {

    }

    @Override
    public void afterBegin(GlobalTransaction tx) {

    }

    @Override
    public void beforeCommit(GlobalTransaction tx) {

    }

    @Override
    public void afterCommit(GlobalTransaction tx) {

    }

    @Override
    public void beforeRollback(GlobalTransaction tx) {

    }

    @Override
    public void afterRollback(GlobalTransaction tx) {

    }

    @Override
    public void afterCompletion(GlobalTransaction tx) {

    }
}
