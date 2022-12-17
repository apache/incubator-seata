package io.seata.rm.tcc.interceptor.parser;

import io.seata.commonapi.interceptor.TxBeanParserUtils;
import io.seata.commonapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.commonapi.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.commonapi.interceptor.parser.InterfaceParser;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.commonapi.remoting.parser.DefaultRemotingParser;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.interceptor.TccActionInterceptorHandler;

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
                    DefaultResourceRegisterParser.get().registerResource(target);
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
