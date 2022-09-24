package io.seata.commonapi.autoproxy;

import java.lang.reflect.Method;

public interface ManualApiExecute {

    void manualApiBefore(Method method, Object[] arguments) throws Throwable;
}
