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
package org.apache.seata.spring.kt

import org.apache.seata.core.context.RootContext
import org.apache.seata.core.exception.TransactionException
import org.apache.seata.core.model.GlobalStatus
import org.apache.seata.core.model.TransactionManager
import org.apache.seata.spring.annotation.GlobalTransactional
import org.apache.seata.spring.kt.support.transactionScope
import org.apache.seata.tm.TransactionManagerHolder
import org.apache.seata.tm.api.GlobalTransactionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TransactionScopeTest {

    companion object {
        private val DEFAULT_XID = "1234567890"
    }

    /**
     * Init.
     */
    init {
        TransactionManagerHolder.set(object : TransactionManager {
            @Throws(TransactionException::class)
            override fun begin(
                applicationId: String,
                transactionServiceGroup: String,
                name: String,
                timeout: Int
            ): String {
                return DEFAULT_XID
            }

            @Throws(TransactionException::class)
            override fun commit(xid: String): GlobalStatus {
                return GlobalStatus.Committed
            }

            @Throws(TransactionException::class)
            override fun rollback(xid: String): GlobalStatus {
                return GlobalStatus.Rollbacked
            }

            @Throws(TransactionException::class)
            override fun getStatus(xid: String): GlobalStatus {
                return GlobalStatus.Begin
            }

            @Throws(TransactionException::class)
            override fun globalReport(xid: String, globalStatus: GlobalStatus): GlobalStatus {
                return globalStatus
            }
        })
    }


    @Test
    @Throws(NoSuchMethodException::class)
    fun testGlobalTransactional() {
        RootContext.bind(DEFAULT_XID)
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        globalTransactionContext.begin()
        println(RootContext.getXID())
        val mockClassAnnotation = MockMethodAnnotation()
        val xid = runBlocking {
            mockClassAnnotation.doBiz()
        }
        Assertions.assertNotNull(xid)
    }

    private open class MockMethodAnnotation {
        /**
         * use io coroutine
         */
        @GlobalTransactional(name = "doBiz")
        suspend fun doBiz(): String? = io {
            return@io RootContext.getXID()
        }

        suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
            return withContext(Dispatchers.IO, block)
        }
    }

    @Test
    @Throws(NoSuchMethodException::class)
    fun testTransactionalScope() {
        RootContext.bind(DEFAULT_XID)
        val globalTransactionContext = GlobalTransactionContext.getCurrentOrCreate()
        globalTransactionContext.begin()
        println(RootContext.getXID())
        val mockMethodScope = MockMethodScope()
        val xid = runBlocking {
            transactionScope {
                mockMethodScope.doBiz()
            }
        }
        Assertions.assertNotNull(xid)
    }

    private open class MockMethodScope {
        /**
         * use io coroutine
         */
        suspend fun doBiz(): String? = io {
            return@io RootContext.getXID()
        }

        suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
            return withContext(Dispatchers.IO, block)
        }
    }
}