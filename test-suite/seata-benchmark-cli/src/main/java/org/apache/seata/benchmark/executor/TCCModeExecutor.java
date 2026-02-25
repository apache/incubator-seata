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
package org.apache.seata.benchmark.executor;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCC mode transaction executor (simplified mock implementation)
 * Note: For Phase 1 (MVP), TCC mode executes empty transactions similar to AT mode.
 * Full TCC implementation with try/confirm/cancel will be added in future versions.
 */
public class TCCModeExecutor extends AbstractTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCModeExecutor.class);

    public TCCModeExecutor(BenchmarkConfig config) {
        super(config);
    }

    @Override
    public void init() {
        LOGGER.info("TCC mode executor initialized (simplified mock mode)");
    }

    @Override
    protected String getTransactionName() {
        return "benchmark-tcc-tx";
    }

    @Override
    protected int getBranchCount() {
        return 0;
    }

    @Override
    protected void executeBusinessLogic() throws Exception {
        // Empty transaction - no TCC operations
        // Full TCC with try/confirm/cancel will be implemented in future versions
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void destroy() {
        LOGGER.info("TCC mode executor destroyed");
    }
}
