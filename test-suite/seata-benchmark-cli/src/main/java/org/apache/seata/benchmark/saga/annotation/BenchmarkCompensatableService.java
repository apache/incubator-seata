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
package org.apache.seata.benchmark.saga.annotation;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.apache.seata.saga.rm.api.SagaTransactional;

/**
 * Benchmark service interface for SAGA_ANNOTATION mode.
 *
 * <p>Each call to {@link #execute} registers one SAGA_ANNOTATION branch with the TC.
 * When the global transaction rolls back, the TC invokes {@link #compensate} for every registered branch.
 */
@SagaTransactional
public interface BenchmarkCompensatableService {

    /**
     * Forward action: registers a SAGA_ANNOTATION branch with the TC via the annotation interceptor.
     *
     * @param context business action context injected by Seata
     * @return true on success
     */
    @CompensationBusinessAction(name = "benchmarkSagaAnnotationAction", compensationMethod = "compensate")
    boolean execute(BusinessActionContext context);

    /**
     * Compensation action: called by the TC for each registered branch on rollback.
     *
     * @param context business action context supplied by the TC
     * @return true when compensation succeeds
     */
    boolean compensate(BusinessActionContext context);
}
