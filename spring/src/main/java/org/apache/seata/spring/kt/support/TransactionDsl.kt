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
package org.apache.seata.spring.kt.support

import org.apache.seata.core.context.RootContext
import org.apache.seata.tm.api.GlobalTransactionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * seata support kotlin coroutine
 *
 * if you use kotlin coroutine, GlobalTransactional can not help you, so you can use:
 * 
 * suspend fun test() = transactionScope {
 *  service1.XXX()
 *  service2.XXX()
 * }
 *
 */
suspend fun <T> transactionScope(block: suspend CoroutineScope.() -> T): T {
    return if (coroutineContext[TransactionCoroutineContext] != null) {
        coroutineScope(block)
    } else {
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        try {
            globalTransactionContext.begin()
            withContext(TransactionCoroutineContext()) {
                block()
            }.also {
                globalTransactionContext.commit()
                RootContext.unbind()
            }
        } catch (e: Exception) {
            globalTransactionContext.rollback()
            RootContext.unbind()
            throw e
        }
    }
}
