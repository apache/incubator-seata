package io.seata.rm.tcc.interceptor.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.commonapi.interceptor.ActionContextUtil;
import io.seata.commonapi.interceptor.TxBeanParserUtils;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.commonapi.interceptor.parser.InterfaceParser;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.commonapi.remoting.parser.DefaultRemotingParser;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class TccActionInterceptorParser implements InterfaceParser {

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target) {
        boolean isTxRemotingBean = TxBeanParserUtils.isTxRemotingBean(target, target.toString());
        if (isTxRemotingBean) {
            RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(target);
            if (remotingDesc != null) {
                if (remotingDesc.isService()) {
                    tryToRegisterResource(remotingDesc);
                }
                if (remotingDesc.isReference()) {
                    //if it is a tcc remote reference
                    Set<String> methodsToProxy = tccProxyTargetMethod(remotingDesc);
                    if (remotingDesc != null && !methodsToProxy.isEmpty()) {
                        Class[] interfaceToProxy = target.getClass().getInterfaces();
                        ProxyInvocationHandler proxyInvocationHandler = new TccActionInterceptorHandler(interfaceToProxy, methodsToProxy);
                        return proxyInvocationHandler;
                    }
                }
            }
        }
        return null;
    }

    private void tryToRegisterResource(RemotingDesc remotingDesc) {
        if (remotingDesc.isService()) {
            try {
                //service bean, registry resource
                Class<?> serviceClass = remotingDesc.getServiceClass();
                Method[] methods = serviceClass.getMethods();
                Object targetBean = remotingDesc.getTargetBean();
                for (Method m : methods) {
                    TwoPhaseBusinessAction twoPhaseBusinessAction = m.getAnnotation(TwoPhaseBusinessAction.class);
                    if (twoPhaseBusinessAction != null) {
                        TCCResource tccResource = new TCCResource();
                        tccResource.setActionName(twoPhaseBusinessAction.name());
                        tccResource.setTargetBean(targetBean);
                        tccResource.setPrepareMethod(m);
                        tccResource.setCommitMethodName(twoPhaseBusinessAction.commitMethod());
                        tccResource.setCommitMethod(serviceClass.getMethod(twoPhaseBusinessAction.commitMethod(),
                                twoPhaseBusinessAction.commitArgsClasses()));
                        tccResource.setRollbackMethodName(twoPhaseBusinessAction.rollbackMethod());
                        tccResource.setRollbackMethod(serviceClass.getMethod(twoPhaseBusinessAction.rollbackMethod(),
                                twoPhaseBusinessAction.rollbackArgsClasses()));
                        // set argsClasses
                        tccResource.setCommitArgsClasses(twoPhaseBusinessAction.commitArgsClasses());
                        tccResource.setRollbackArgsClasses(twoPhaseBusinessAction.rollbackArgsClasses());
                        // set phase two method's keys
                        tccResource.setPhaseTwoCommitKeys(this.getTwoPhaseArgs(tccResource.getCommitMethod(),
                                twoPhaseBusinessAction.commitArgsClasses()));
                        tccResource.setPhaseTwoRollbackKeys(this.getTwoPhaseArgs(tccResource.getRollbackMethod(),
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

    protected String[] getTwoPhaseArgs(Method method, Class<?>[] argsClasses) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] keys = new String[parameterAnnotations.length];
        /*
         * get parameter's key
         * if method's parameter list is like
         * (BusinessActionContext, @BusinessActionContextParameter("a") A a, @BusinessActionContextParameter("b") B b)
         * the keys will be [null, a, b]
         */
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (int j = 0; j < parameterAnnotations[i].length; j++) {
                if (parameterAnnotations[i][j] instanceof BusinessActionContextParameter) {
                    BusinessActionContextParameter param = (BusinessActionContextParameter) parameterAnnotations[i][j];
                    String key = ActionContextUtil.getParamNameFromAnnotation(param);
                    keys[i] = key;
                    break;
                }
            }
            if (keys[i] == null && !(argsClasses[i].equals(BusinessActionContext.class))) {
                throw new IllegalArgumentException("non-BusinessActionContext parameter should use annotation " +
                        "BusinessActionContextParameter");
            }
        }
        return keys;
    }

    /**
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param remotingDesc the remoting desc
     * @return boolean boolean
     */
    private Set<String> tccProxyTargetMethod(RemotingDesc remotingDesc) {
        if (!remotingDesc.isReference() || remotingDesc == null) {
            return Collections.emptySet();
        }
        Set<String> methodsToProxy = new HashSet<>();
        //check if it is TCC bean
        Class<?> tccServiceClazz = remotingDesc.getServiceClass();
        Method[] methods = tccServiceClazz.getMethods();
        TwoPhaseBusinessAction twoPhaseBusinessAction;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if (twoPhaseBusinessAction != null) {
                methodsToProxy.add(method.getName());
            }
        }
        if (methodsToProxy.isEmpty()) {
            return Collections.emptySet();
        }
        // sofa:reference /  dubbo:reference, AOP
        return methodsToProxy;
    }
}
