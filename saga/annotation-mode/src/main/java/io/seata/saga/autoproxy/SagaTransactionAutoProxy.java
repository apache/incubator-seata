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
package io.seata.saga.autoproxy;

import io.seata.common.exception.FrameworkException;
import io.seata.rm.DefaultResourceManager;
import io.seata.saga.api.SagaTransactional;
import io.seata.saga.interceptor.SagaActionInterceptor;
import io.seata.saga.rm.annotation.SagaAnnotationResource;
import io.seata.spring.autoproxy.IsTransactionProxyResult;
import io.seata.spring.autoproxy.TransactionAutoProxy;
import io.seata.spring.remoting.Protocols;
import io.seata.spring.remoting.RemotingDesc;
import io.seata.spring.remoting.parser.DefaultRemotingParser;

import java.lang.reflect.Method;

/**
 * the saga implements of TransactionAutoProxy
 *
 * @author ruishansun
 */
public class SagaTransactionAutoProxy implements TransactionAutoProxy {

    @Override
    public IsTransactionProxyResult isTransactionProxyTargetBean(RemotingDesc remotingDesc) {

        if (remotingDesc == null) {
            return new IsTransactionProxyResult();
        }
        //check if it is saga bean
        Class<?> sagaInterfaceClazz = remotingDesc.getInterfaceClass();
        Method[] methods = sagaInterfaceClazz.getMethods();
        for (Method method : methods) {
            SagaTransactional sagaTransactional = method.getAnnotation(SagaTransactional.class);
            if (sagaTransactional != null && (Protocols.IN_JVM == remotingDesc.getProtocol() || remotingDesc.isReference())) {
                this.registryResource(remotingDesc);
                IsTransactionProxyResult result = new IsTransactionProxyResult();
                result.setProxyTargetBean(true);
                result.setUseCommonFence(sagaTransactional.useCommonFence());
                result.setMethodInterceptor(new SagaActionInterceptor(remotingDesc));
                return result;
            }
        }
        return new IsTransactionProxyResult();
    }

    /**
     * register saga resource
     *
     * @param remotingDesc the remotingDesc
     */
    private void registryResource(RemotingDesc remotingDesc) {
        if (!remotingDesc.isReference()) {
            try {
                Class<?> interfaceClass = remotingDesc.getInterfaceClass();
                Method[] methods = interfaceClass.getMethods();
                //service bean, registry resource
                Object targetBean = remotingDesc.getTargetBean();
                for (Method m : methods) {
                    SagaTransactional sagaTransactional = m.getAnnotation(SagaTransactional.class);
                    if (sagaTransactional != null) {
                        SagaAnnotationResource sagaAnnotationResource = new SagaAnnotationResource();
                        sagaAnnotationResource.setActionName(sagaTransactional.name());
                        sagaAnnotationResource.setTargetBean(targetBean);
                        sagaAnnotationResource.setCommitMethod(m);
                        sagaAnnotationResource.setCompensationMethodName(sagaTransactional.compensationMethod());
                        sagaAnnotationResource.setCompensationMethod(interfaceClass.getMethod(sagaTransactional.compensationMethod(),
                                sagaTransactional.compensationArgsClasses()));
                        // set argsClasses
                        sagaAnnotationResource.setCompensationArgsClasses(sagaTransactional.compensationArgsClasses());
                        // set phase two method's keys
                        sagaAnnotationResource.setPhaseTwoCompensationKeys(DefaultRemotingParser.get().getTwoPhaseArgs(sagaAnnotationResource.getCompensationMethod(),
                                sagaTransactional.compensationArgsClasses()));
                        //registry tcc resource
                        DefaultResourceManager.get().registerResource(sagaAnnotationResource);
                    }
                }
            } catch (Throwable t) {
                throw new FrameworkException(t, "parser remoting service error");
            }
        }
    }
}
