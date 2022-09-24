package io.seata.manualapi.api;

import io.seata.manualapi.advisor.DefaultSeataAdvisor;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class SeataClient<T> {

    public static void init(String applicationId, String txServiceGroup) {
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);
    }

    public static <T> T createProxy(Class<T> tccAction) throws InstantiationException, IllegalAccessException {

        return new ByteBuddy()
                .subclass(tccAction)
                .method(ElementMatchers.any())
                .intercept(Advice.to(DefaultSeataAdvisor.class))
                .make()
                .load(tccAction.getClassLoader())
                .getLoaded()
                .newInstance();
    }
}
