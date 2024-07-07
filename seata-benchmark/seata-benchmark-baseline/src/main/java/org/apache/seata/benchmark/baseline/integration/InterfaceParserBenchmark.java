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
package org.apache.seata.benchmark.baseline.integration;

import org.apache.seata.benchmark.baseline.model.TccAction;
import org.apache.seata.benchmark.baseline.model.TccActionImpl;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.TCCResourceManager;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(value = Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class InterfaceParserBenchmark {

    @Benchmark
    public void tccProxyInvocationHandler() throws Exception {
        TccActionImpl target = new TccActionImpl();
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target, TccAction.class.getName());
        assert proxyInvocationHandler != null;
    }

    @TearDown(Level.Invocation)
    public void clear() {
        // clear tcc resource, prevent resourceId duplication
        TCCResourceManager tccResourceManager = (TCCResourceManager) DefaultResourceManager.get().getResourceManager(BranchType.TCC);
        tccResourceManager.getManagedResources().clear();
    }

}