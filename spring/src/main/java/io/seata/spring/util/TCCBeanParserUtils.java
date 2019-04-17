package io.seata.spring.util;

import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.rm.tcc.remoting.Protocols;
import io.seata.rm.tcc.remoting.RemotingDesc;
import io.seata.rm.tcc.remoting.parser.DefaultRemotingParser;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * parser TCC bean
 *
 * @author zhangsen
 * @data 2019 /3/18
 */
public class TCCBeanParserUtils {

    /**
     * is auto proxy TCC bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @param applicationContext the application context
     * @return boolean boolean
     */
    public static boolean isTccAutoProxy(Object bean, String beanName, ApplicationContext applicationContext){
        RemotingDesc remotingDesc = null;
        boolean isRemotingBean = parserRemotingServiceInfo(bean, beanName);
        //is remoting bean
        if(isRemotingBean) {
            remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
            if(remotingDesc != null && remotingDesc.getProtocol() == Protocols.IN_JVM){
                //LocalTCC
                return isTccProxyTargetBean(remotingDesc);
            }else {
                // sofa:reference / dubbo:reference, factory bean
                return false;
            }
        }else{
            //get RemotingBean description
            remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
            if(remotingDesc == null){
                //check FactoryBean
                if(isRemotingFactoryBean(bean, beanName, applicationContext)){
                    remotingDesc = DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
                    return isTccProxyTargetBean(remotingDesc);
                }else {
                    return false;
                }
            }else {
                return isTccProxyTargetBean(remotingDesc);
            }
        }
    }

    /**
     * if it is proxy bean, check if the FactoryBean is Remoting bean
     *
     * @param bean               the bean
     * @param beanName           the bean name
     * @param applicationContext the application context
     * @return boolean boolean
     */
    protected static boolean isRemotingFactoryBean(Object bean, String beanName, ApplicationContext applicationContext) {
        if(!SpringProxyUtils.isProxy(bean)){
            return false;
        }
        //the FactoryBean of proxy bean
        String factoryBeanName = new StringBuilder().append("&").append(beanName).toString();
        Object factoryBean = null;
        if(applicationContext != null && applicationContext.containsBean(factoryBeanName)){
            factoryBean = applicationContext.getBean(factoryBeanName);
        }
        //not factory bean，needn't proxy
        if(factoryBean == null ){
            return false;
        }
        //get FactoryBean info
        return parserRemotingServiceInfo(factoryBean, beanName);
    }

    /**
     * is TCC proxy-bean/target-bean: LocalTCC , the proxy bean of sofa:reference/dubbo:reference
     *
     * @param remotingDesc the remoting desc
     * @return boolean boolean
     */
    protected static boolean isTccProxyTargetBean(RemotingDesc remotingDesc){
        if(remotingDesc == null) {
            return false;
        }
        //check if it is TCC bean
        boolean isTccClazz = false;
        Class<?> tccInterfaceClazz = remotingDesc.getInterfaceClass();
        Method[] methods = tccInterfaceClazz.getMethods();
        TwoPhaseBusinessAction twoPhaseBusinessAction = null;
        for (Method method : methods) {
            twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
            if(twoPhaseBusinessAction != null ){
                isTccClazz = true;
                break;
            }
        }
        if(!isTccClazz){
            return false;
        }
        short protocols = remotingDesc.getProtocol();
        //LocalTCC
        if(Protocols.IN_JVM == protocols){
            //in jvm TCC bean , AOP
            return true;
        }
        // sofa:reference /  dubbo:reference, AOP
        if(remotingDesc.isReference()){
            return true;
        }
        return false;
    }

    /**
     * get remoting bean info: sofa:service、sofa:reference、dubbo:reference、dubbo:service
     *
     * @param bean     the bean
     * @param beanName the bean name
     * @return if sofa:service、sofa:reference、dubbo:reference、dubbo:service return true，else return false
     */
    protected static boolean parserRemotingServiceInfo(Object bean, String beanName) {
        if(DefaultRemotingParser.get().isRemoting(bean, beanName)) {
            return null != DefaultRemotingParser.get().parserRemotingServiceInfo(bean, beanName);
        }
        return false;
    }

    /**
     * get the remoting description of TCC bean
     *
     * @param beanName the bean name
     * @return remoting desc
     */
    public static  RemotingDesc getRemotingDesc(String beanName){
        return DefaultRemotingParser.get().getRemotingBeanDesc(beanName);
    }
}
