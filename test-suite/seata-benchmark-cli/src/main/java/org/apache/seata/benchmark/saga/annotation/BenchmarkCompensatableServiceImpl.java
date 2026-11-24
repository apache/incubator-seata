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

/**
 * Benchmark implementation of {@link BenchmarkCompensatableService}.
 *
 * <p>Both the forward action and the compensation are no-ops so that benchmark
 * results reflect pure Seata SAGA_ANNOTATION protocol overhead rather than
 * any application-level business logic cost.
 */
public class BenchmarkCompensatableServiceImpl implements BenchmarkCompensatableService {

    @Override
    public boolean execute(BusinessActionContext context) {
        // No-op: measures annotation interceptor + TC branch-registration overhead only
        return true;
    }

    @Override
    public boolean compensate(BusinessActionContext context) {
        // No-op: measures TC compensation-callback overhead only
        return true;
    }
}
