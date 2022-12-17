package io.seata.rm.tcc.resource.parser;

import io.seata.common.exception.FrameworkException;
import io.seata.commonapi.interceptor.ActionContextUtil;
import io.seata.commonapi.interceptor.TxBeanParserUtils;
import io.seata.commonapi.interceptor.parser.RegisterResourceParser;
import io.seata.commonapi.remoting.RemotingDesc;
import io.seata.commonapi.remoting.parser.DefaultRemotingParser;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author leezongjie
 * @date 2022/12/17
 */
public class TccRegisterResourceParser implements RegisterResourceParser {

    @Override
    public void registerResource(Object target) {
        boolean isTxRemotingBean = TxBeanParserUtils.isTxRemotingBean(target, target.toString());
        if (isTxRemotingBean) {
            RemotingDesc remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(target);
            if (remotingDesc != null) {
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

}
