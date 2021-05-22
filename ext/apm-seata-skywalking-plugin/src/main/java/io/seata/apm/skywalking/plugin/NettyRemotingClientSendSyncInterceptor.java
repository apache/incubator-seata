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
package io.seata.apm.skywalking.plugin;

import io.netty.channel.Channel;
import io.seata.core.protocol.RpcMessage;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import io.seata.apm.skywalking.plugin.common.SWSeataUtils;

import java.lang.reflect.Method;

/**
 * @author zhaoyuguang
 */
public class NettyRemotingClientSendSyncInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        if (allArguments[0] == null) {
            return;
        }
        Channel channel = (Channel) allArguments[0];
        String peer = SWSeataUtils.convertPeer(channel);
        RpcMessage rpcMessage = (RpcMessage) allArguments[1];
        String operationName = SWSeataUtils.convertOperationName(rpcMessage);
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan activeSpan = ContextManager.createExitSpan(operationName, contextCarrier, peer);
//        activeSpan.setComponent(ComponentsDefine.SEATA);
        activeSpan.setPeer(peer);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            rpcMessage.getHeadMap().put(next.getHeadKey(), next.getHeadValue());
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (allArguments[0] != null) {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }
}
