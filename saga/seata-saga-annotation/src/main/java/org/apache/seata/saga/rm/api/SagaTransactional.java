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
package org.apache.seata.saga.rm.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SagaTransactional annotation for marking Saga transaction participant beans.
 *
 * This annotation is specifically designed for Saga transaction scenarios, providing
 * clearer semantic meaning compared to @LocalTCC when used in Saga compensation patterns.
 * It enables proper proxy enhancement and integration with Saga transaction management.
 *
 * Purpose:
 * - Marks services that participate in Saga transactions
 * - Enables automatic proxy creation for compensation action support
 * - Provides clear semantic separation from TCC-specific functionality
 * - Supports both interface and implementation class annotation
 *
 * Key Benefits over @LocalTCC in Saga scenarios:
 * 1. Semantic Clarity: "Saga" clearly indicates compensation-based transaction mode
 * 2. Purpose-built: Designed specifically for Saga patterns, not retrofitted from TCC
 * 3. Future-proof: Can evolve independently to support Saga-specific features
 * 4. Maintainability: Makes codebase intentions clearer for developers
 *
 * Typical Usage Pattern:
 *
 * @SagaTransactional
 * public interface OrderSagaService {
 *
 *     @CompensationBusinessAction(
 *         name = "createOrder",
 *         compensationMethod = "cancelOrder"
 *     )
 *     OrderResult createOrder(BusinessActionContext context, CreateOrderRequest request);
 *
 *     boolean cancelOrder(BusinessActionContext context);
 * }
 *
 * Advanced Usage with Multiple Actions:
 *
 * @SagaTransactional
 * public interface PaymentSagaService {
 *
 *     @CompensationBusinessAction(name = "reserveAmount", compensationMethod = "releaseAmount")
 *     boolean reserveAmount(BusinessActionContext context, String accountId, BigDecimal amount);
 *
 *     @CompensationBusinessAction(name = "deductAmount", compensationMethod = "refundAmount")
 *     boolean deductAmount(BusinessActionContext context, String accountId, BigDecimal amount);
 *
 *     boolean releaseAmount(BusinessActionContext context);
 *     boolean refundAmount(BusinessActionContext context);
 * }
 *
 * Annotation Placement:
 * - Interface level: Recommended for service contracts
 * - Implementation class level: Alternative for concrete classes
 * - Inheritance: Annotations are inherited from parent classes/interfaces
 *
 * Integration with Spring Framework:
 * This annotation works seamlessly with Spring's component scanning and proxy mechanisms.
 * Services annotated with @SagaTransactional will be automatically detected and enhanced
 * by Seata's runtime infrastructure.
 *
 * Backward Compatibility:
 * - Existing @LocalTCC annotations continue to work unchanged
 * - Both annotations can coexist in the same application
 * - No migration is required for existing TCC implementations
 * - New Saga implementations should prefer @SagaTransactional
 *
 * Performance Characteristics:
 * - Zero runtime overhead compared to @LocalTCC
 * - Efficient annotation scanning with caching
 * - Optimized for high-throughput Saga scenarios
 *
 * @see org.apache.seata.saga.rm.api.CompensationBusinessAction Primary annotation for defining compensation actions
 * @see org.apache.seata.rm.tcc.api.LocalTCC Legacy TCC-specific annotation (still supported)
 * @see org.apache.seata.rm.tcc.api.BusinessActionContext Context parameter passed to compensation methods
 * @see org.apache.seata.saga.rm.remoting.parser.SagaTransactionalRemotingParser Parser that handles this annotation
 * @since 2.5.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SagaTransactional {}
