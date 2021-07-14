/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.apm.skywalking.plugin.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.MultiClassNameMatch.byMultiClassMatch;

/**
 * @author zhaoyuguang
 */
public class RemotingProcessorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String INTERCEPTOR_CLASS = "io.seata.apm.skywalking.plugin.RemotingProcessorProcessInterceptor";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("process");
                }

                @Override
                public String getMethodsInterceptor() {
                    return INTERCEPTOR_CLASS;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return byMultiClassMatch("io.seata.core.rpc.processor.client.ClientHeartbeatProcessor",
            "io.seata.core.rpc.processor.client.ClientOnResponseProcessor",
            "io.seata.core.rpc.processor.client.RmBranchCommitProcessor",
            "io.seata.core.rpc.processor.client.RmBranchRollbackProcessor",
            "io.seata.core.rpc.processor.client.RmUndoLogProcessor",
            "io.seata.core.rpc.processor.server.RegRmProcessor",
            "io.seata.core.rpc.processor.server.RegTmProcessor",
            "io.seata.core.rpc.processor.server.ServerHeartbeatProcessor",
            "io.seata.core.rpc.processor.server.ServerOnRequestProcessor",
            "io.seata.core.rpc.processor.server.ServerOnResponseProcessor");
    }
}
