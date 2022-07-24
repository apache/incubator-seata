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
package io.seata.rm.tcc.autoproxy;

import io.seata.common.exception.FrameworkException;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.TccActionInterceptor;
import io.seata.spring.autoproxy.IsTransactionProxyResult;
import io.seata.spring.autoproxy.TransactionAutoProxy;
import io.seata.spring.remoting.Protocols;
import io.seata.spring.remoting.RemotingDesc;
import io.seata.spring.remoting.parser.DefaultRemotingParser;

import java.lang.reflect.Method;

/**
 * the tcc implements of TransactionAutoProxy
 *
 * @author ruishansun
 */
public class TccTransactionAutoProxy implements TransactionAutoProxy {

    /**
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param remotingDesc the remoting desc
     * @return boolean
     */
    @Override
    public IsTransactionProxyResult isTransactionProxyTargetBean(RemotingDesc remotingDesc) {
        if (remotingDesc == null) {
            return new IsTransactionProxyResult();
        }
        //check if it is TCC bean
        boolean isTccClazz = false;
        boolean userFence = false;
        Class<?> tccInterfaceClazz = remotingDesc.getInterfaceClass();
        Method[] methods = tccInterfaceClazz.getMethods();
        TwoPhaseBusinessAction twoPhaseBusinessAction;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if (twoPhaseBusinessAction != null) {
                isTccClazz = true;
                if (twoPhaseBusinessAction.useTCCFence()) {
                    userFence = true;
                }
                break;
            }
        }
        if (!isTccClazz) {
            return new IsTransactionProxyResult();
        }
        short protocols = remotingDesc.getProtocol();
        //LocalTCC
        if (Protocols.IN_JVM == protocols) {
            this.registryResource(remotingDesc);
            //in jvm TCC bean , AOP
            IsTransactionProxyResult result = new IsTransactionProxyResult();
            result.setProxyTargetBean(true);
            result.setUseCommonFence(userFence);
            result.setMethodInterceptor(new TccActionInterceptor(remotingDesc));
            return result;
        }
        // sofa:reference /  dubbo:reference, AOP
        if (remotingDesc.isReference()) {
            this.registryResource(remotingDesc);
            IsTransactionProxyResult result = new IsTransactionProxyResult();
            result.setProxyTargetBean(true);
            result.setUseCommonFence(userFence);
            result.setMethodInterceptor(new TccActionInterceptor(remotingDesc));
            return result;
        } else {
            return new IsTransactionProxyResult();
        }
    }

    private void registryResource(RemotingDesc remotingDesc) {
        if (remotingDesc.isService()) {
            try {
                Class<?> interfaceClass = remotingDesc.getInterfaceClass();
                Method[] methods = interfaceClass.getMethods();
                //service bean, registry resource
                Object targetBean = remotingDesc.getTargetBean();
                for (Method m : methods) {
                    TwoPhaseBusinessAction twoPhaseBusinessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
                    if (twoPhaseBusinessAction != null) {
                        TCCResource tccResource = new TCCResource();
                        tccResource.setActionName(twoPhaseBusinessAction.name());
                        tccResource.setTargetBean(targetBean);
                        tccResource.setPrepareMethod(m);
                        tccResource.setCommitMethodName(twoPhaseBusinessAction.commitMethod());
                        tccResource.setCommitMethod(interfaceClass.getMethod(twoPhaseBusinessAction.commitMethod(),
                                twoPhaseBusinessAction.commitArgsClasses()));
                        tccResource.setRollbackMethodName(twoPhaseBusinessAction.rollbackMethod());
                        tccResource.setRollbackMethod(interfaceClass.getMethod(twoPhaseBusinessAction.rollbackMethod(),
                                twoPhaseBusinessAction.rollbackArgsClasses()));
                        // set argsClasses
                        tccResource.setCommitArgsClasses(twoPhaseBusinessAction.commitArgsClasses());
                        tccResource.setRollbackArgsClasses(twoPhaseBusinessAction.rollbackArgsClasses());
                        // set phase two method's keys
                        tccResource.setPhaseTwoCommitKeys(DefaultRemotingParser.get().getTwoPhaseArgs(tccResource.getCommitMethod(),
                                twoPhaseBusinessAction.commitArgsClasses()));
                        tccResource.setPhaseTwoRollbackKeys(DefaultRemotingParser.get().getTwoPhaseArgs(tccResource.getRollbackMethod(),
                                twoPhaseBusinessAction.rollbackArgsClasses()));
                        //registry tcc resource
                        DefaultResourceManager.get().registerResource(tccResource);
                    }
                }
            } catch (Throwable t) {
                throw new FrameworkException(t, "parser remoting service error");
            }
        }
    }
}