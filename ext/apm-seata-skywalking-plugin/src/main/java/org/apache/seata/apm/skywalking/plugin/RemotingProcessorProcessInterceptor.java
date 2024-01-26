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
package org.apache.seata.apm.skywalking.plugin;

import com.alipay.sofa.common.profile.StringUtil;
import org.apache.seata.apm.skywalking.plugin.common.SWSeataUtils;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

/**
 * The RemoteProcessor deal.
 *
 */
public class RemotingProcessorProcessInterceptor implements
        InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method,
                             Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        RpcMessage rpcMessage = (RpcMessage) allArguments[1];
        String operationName = SWSeataUtils.convertOperationName(rpcMessage);
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(rpcMessage.getHeadMap().get(next.getHeadKey()));
        }
        AbstractSpan activeSpan = ContextManager.createEntrySpan(operationName, contextCarrier);
        SpanLayer.asRPCFramework(activeSpan);
        activeSpan.setComponent(ComponentsDefine.SEATA);

        String xid = SWSeataUtils.convertXid(rpcMessage);
        if (StringUtil.isNotBlank(xid)) {
            activeSpan.tag(new StringTag(20, "Seata.xid"), xid);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method,
                              Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        RpcMessage rpcMessage = (RpcMessage) allArguments[0];
        if (rpcMessage.getBody() instanceof AbstractMessage) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst,
                                      Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }
}
