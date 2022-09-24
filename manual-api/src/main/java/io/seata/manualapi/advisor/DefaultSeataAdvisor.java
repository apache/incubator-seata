package io.seata.manualapi.advisor;

import io.seata.commonapi.autoproxy.IsTransactionProxyResult;
import io.seata.commonapi.interceptor.TxBeanParserUtils;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class DefaultSeataAdvisor {

    @Advice.OnMethodEnter
    public static void onMethodEnter(@Advice.Origin Method method, @Advice.AllArguments Object[] arguments, @Advice.This Object bean) throws Throwable {

        IsTransactionProxyResult isTransactionProxyResult = TxBeanParserUtils.getManualProxyResult(bean, bean.getClass().getName());
        if (isTransactionProxyResult.isProxyTargetBean()) {
            // init tcc fence clean task if enable useTccFence
//            TxBeanParserUtils.initCommonFenceCleanTask(TxBeanParserUtils.getRemotingDesc(beanName), applicationContext, isTransactionProxyResult.isUseCommonFence());
            isTransactionProxyResult.getManualApiExecute().manualApiBefore(method, arguments);
        }
    }
}
