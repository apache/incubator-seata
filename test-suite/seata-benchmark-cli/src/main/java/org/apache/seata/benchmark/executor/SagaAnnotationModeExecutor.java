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
import org.apache.seata.benchmark.saga.annotation.BenchmarkCompensatableService;
import org.apache.seata.benchmark.saga.annotation.BenchmarkCompensatableServiceImpl;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.saga.rm.interceptor.parser.SagaAnnotationActionInterceptorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * SAGA_ANNOTATION mode transaction executor.
 *
 * <p>Supports two sub-modes controlled by {@code --branches}:
 * <ul>
 *   <li><b>Empty mode</b> ({@code branches == 0}): starts and commits an empty global transaction.
 *       Measures pure Seata protocol overhead with no branch registration.</li>
 *   <li><b>Real mode</b> ({@code branches > 0}): registers {@code branches} SAGA_ANNOTATION
 *       branches per transaction via the {@link BenchmarkCompensatableService} proxy.
 *       On rollback the TC invokes the compensation method for every registered branch.</li>
 * </ul>
 *
 * <p>No Spring container is required. A JDK dynamic proxy wraps
 * {@link org.apache.seata.saga.rm.interceptor.SagaAnnotationActionInterceptorHandler},
 * which is equivalent to what Spring AOP would produce at runtime.
 * {@link org.apache.seata.saga.rm.SagaAnnotationResourceManager} is loaded automatically
 * via SPI when the {@code seata-saga-annotation} module is on the classpath.
 */
public class SagaAnnotationModeExecutor extends AbstractTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaAnnotationModeExecutor.class);

    private BenchmarkCompensatableService serviceProxy;
    private BenchmarkCompensatableServiceImpl serviceImpl;

    public SagaAnnotationModeExecutor(BenchmarkConfig config) {
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
            LOGGER.info("SAGA_ANNOTATION mode executor initialized (empty transaction mode)");
        }
    }

    private void initRealMode() {
        LOGGER.info("Initializing SAGA_ANNOTATION mode executor (annotation-based compensation)");

        serviceImpl = new BenchmarkCompensatableServiceImpl();

        // SagaAnnotationActionInterceptorParser does two things:
        // 1. Registers SagaAnnotationResource with DefaultResourceManager (enables TC callbacks)
        // 2. Returns a SagaAnnotationActionInterceptorHandler for proxy dispatch
        SagaAnnotationActionInterceptorParser parser = new SagaAnnotationActionInterceptorParser();
        ProxyInvocationHandler handler = parser.parserInterfaceToProxy(serviceImpl, "benchmarkSagaAnnotationService");

        // Wrap the handler with a JDK dynamic proxy — equivalent to Spring AOP proxy
        serviceProxy = (BenchmarkCompensatableService) Proxy.newProxyInstance(
                BenchmarkCompensatableService.class.getClassLoader(),
                new Class[] {BenchmarkCompensatableService.class},
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
                        return serviceImpl;
                    }

                    @Override
                    public Object[] getArguments() {
                        return args;
                    }

                    @Override
                    public Object proceed() throws Throwable {
                        return method.invoke(serviceImpl, args);
                    }
                }));

        LOGGER.info("SAGA_ANNOTATION mode executor initialized ({} branches per transaction)", config.getBranches());
    }

    @Override
    protected void executeBusinessLogic() throws Exception {
        if (!isRealMode()) {
            return;
        }
        // Each proxy call triggers the annotation interceptor which registers
        // one SAGA_ANNOTATION branch with the TC.
        for (int i = 0; i < config.getBranches(); i++) {
            serviceProxy.execute(null);
        }
    }

    @Override
    protected String getTransactionName() {
        return "benchmark-saga-annotation-tx";
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
        serviceProxy = null;
        serviceImpl = null;
        LOGGER.info("SAGA_ANNOTATION mode executor destroyed");
    }
}
