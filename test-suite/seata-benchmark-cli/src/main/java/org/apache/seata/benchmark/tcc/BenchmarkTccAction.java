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
package org.apache.seata.benchmark.tcc;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Benchmark TCC action interface.
 *
 * <p>Used by {@link org.apache.seata.benchmark.executor.TCCModeExecutor} in real mode
 * ({@code branches > 0}) to drive the TCC protocol path through the Seata interceptor.
 * Each call to {@link #prepare} registers one TCC branch with the TC. On global commit
 * the TC invokes {@link #commit}; on global rollback it invokes {@link #rollback}.
 *
 * <p>All three methods are intentionally no-ops so the benchmark measures only Seata
 * protocol overhead, not business logic.
 */
@LocalTCC
public interface BenchmarkTccAction {

    String ACTION_NAME = "seata-benchmark-cli-benchmarkTccAction";

    @TwoPhaseBusinessAction(name = ACTION_NAME, commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext context);

    boolean commit(BusinessActionContext context);

    boolean rollback(BusinessActionContext context);
}
