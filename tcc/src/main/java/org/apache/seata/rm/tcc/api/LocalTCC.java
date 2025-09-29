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
package org.apache.seata.rm.tcc.api;

import org.apache.seata.rm.tcc.remoting.parser.LocalTCCRemotingParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Local TCC bean annotation, add on the TCC interface
 *
 * This annotation is specifically designed for TCC (Try-Confirm-Cancel) transaction mode.
 * For Saga scenarios, consider using @SagaTransactional annotation instead to avoid confusion.
 *
 * When to Use @LocalTCC:
 * - Pure TCC scenarios with Try-Confirm-Cancel semantics
 * - Existing stable implementations (backward compatibility)
 * - Services explicitly designed for TCC mode
 * - When you want to explicitly indicate TCC transaction mode
 *
 * When to Consider @SagaTransactional Instead:
 * - Saga scenarios with compensation actions
 * - Generic transaction participants (non-TCC specific)
 * - Services that might be used in multiple transaction modes
 *
 * Example Usage:
 *
 * @LocalTCC
 * public interface PaymentTccService {
 *     @TwoPhaseBusinessAction(name = "payment", commitMethod = "confirmPayment", rollbackMethod = "cancelPayment")
 *     boolean tryPayment(BusinessActionContext context, String orderId, double amount);
 *
 *     boolean confirmPayment(BusinessActionContext context);
 *
 *     boolean cancelPayment(BusinessActionContext context);
 * }
 *
 * Annotation Separation:
 * Starting from version 2.5.0, @LocalTCC and @SagaTransactional have dedicated parsers:
 * - LocalTCCRemotingParser handles @LocalTCC (TCC mode)
 * - SagaTransactionalRemotingParser handles @SagaTransactional (Saga mode)
 * This ensures clear separation of concerns and better maintainability.
 *
 * @see org.apache.seata.spring.annotation.GlobalTransactionScanner#wrapIfNecessary(Object, String, Object) the scanner for TM, GlobalLock, and TCC mode
 * @see LocalTCCRemotingParser the RemotingParser impl for LocalTCC
 * @see org.apache.seata.saga.rm.api.SagaTransactional the dedicated annotation for Saga transaction participants
 * @see org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction commonly used with this annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface LocalTCC {}
