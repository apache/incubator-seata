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
import org.apache.seata.benchmark.tcc.BenchmarkTccAction;
import org.apache.seata.benchmark.tcc.BenchmarkTccActionImpl;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.rm.tcc.interceptor.parser.TccActionInterceptorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * TCC mode transaction executor.
 *
 * <p>Supports two sub-modes controlled by {@code --branches}:
 * <ul>
 *   <li><b>Empty mode</b> ({@code branches == 0}): starts and commits an empty global
 *       transaction. Measures pure Seata protocol overhead with no branch registration.</li>
 *   <li><b>Real mode</b> ({@code branches > 0}): registers {@code branches} TCC branches
 *       per transaction via the {@link BenchmarkTccAction} proxy. On commit the TC invokes
 *       {@link BenchmarkTccAction#commit} for every registered branch; on rollback it invokes
 *       {@link BenchmarkTccAction#rollback}.</li>
 * </ul>
 *
 * <p>No Spring container is required. A JDK dynamic proxy wraps
 * {@link org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler},
 * which is equivalent to what Spring AOP would produce at runtime.
 */
public class TCCModeExecutor extends AbstractTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCModeExecutor.class);

    private BenchmarkTccAction actionProxy;
    private BenchmarkTccActionImpl actionImpl;

    public TCCModeExecutor(BenchmarkConfig config) {
        super(config);
    }

    private boolean isRealMode() {
        return config.getBranches() > 0;
    }

    @Override
    public void init() {
        if (isRealMode()) {
            initRealMode();
        } else {
            LOGGER.info("TCC mode executor initialized (empty transaction mode)");
        }
    }

    private void initRealMode() {
        LOGGER.info("Initializing TCC mode executor (try/confirm/cancel)");

        actionImpl = new BenchmarkTccActionImpl();

        // TccActionInterceptorParser does two things:
        // 1. Registers TCCResource with DefaultResourceManager (enables TC callbacks)
        // 2. Returns a TccActionInterceptorHandler for proxy dispatch
        TccActionInterceptorParser parser = new TccActionInterceptorParser();
        ProxyInvocationHandler handler;
        try {
            handler = parser.parserInterfaceToProxy(actionImpl, "benchmarkTccService");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TCC proxy", e);
        }
        if (handler == null) {
            throw new IllegalStateException(
                    "Failed to initialize TCC proxy: no @TwoPhaseBusinessAction method was found for "
                            + BenchmarkTccAction.class.getName());
        }

        // Wrap the handler with a JDK dynamic proxy — equivalent to Spring AOP proxy
        actionProxy = (BenchmarkTccAction) Proxy.newProxyInstance(
                BenchmarkTccAction.class.getClassLoader(),
                new Class[] {BenchmarkTccAction.class},
                (proxy, method, args) -> handler.invoke(new InvocationWrapper() {
                    @Override
                    public Method getMethod() {
                        return method;
                    }

                    @Override
                    public Object getProxy() {
                        return proxy;
                    }

                    @Override
                    public Object getTarget() {
                        return actionImpl;
                    }

                    @Override
                    public Object[] getArguments() {
                        return args;
                    }

                    @Override
                    public Object proceed() throws Throwable {
                        return method.invoke(actionImpl, args);
                    }
                }));

        LOGGER.info("TCC mode executor initialized ({} branches per transaction)", config.getBranches());
    }

    @Override
    protected void executeBusinessLogic() throws Exception {
        if (!isRealMode()) {
            return;
        }
        // Each proxy call triggers the TCC interceptor which registers
        // one TCC branch with the TC via branchRegister RPC.
        for (int i = 0; i < config.getBranches(); i++) {
            actionProxy.prepare(null);
        }
    }

    @Override
    protected String getTransactionName() {
        return "benchmark-tcc-tx";
    }

    @Override
    protected int getBranchCount() {
        return config.getBranches();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void destroy() {
        actionProxy = null;
        actionImpl = null;
        LOGGER.info("TCC mode executor destroyed");
    }
}
