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
package io.seata.spring.annotation.datasource;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.SeataDataSourceProxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.IntroductionInfo;

/**
 * @author xingfudeshi@gmail.com
 * @author selfishlover
 */
public class SeataAutoDataSourceProxyAdvice implements MethodInterceptor, IntroductionInfo {

    private final BranchType branchType;

    private final Class<?>[] attachedInterfaces = new Class[]{SeataProxy.class};

    public SeataAutoDataSourceProxyAdvice(String dataSourceProxyMode) {
        this.branchType = BranchType.get(dataSourceProxyMode);

        //Set the default branch type in the RootContext.
        RootContext.setDefaultBranchType(this.branchType);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // check whether current context is expected
        if (!inExpectedContext()) {
            return invocation.proceed();
        }

        Method method = invocation.getMethod();
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        Method declared;
        try {
            declared = DataSource.class.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            // mean this method is not declared by DataSource
            return invocation.proceed();
        }

        // switch invoke instance to its proxy
        DataSource origin = (DataSource) invocation.getThis();
        SeataDataSourceProxy proxy = DataSourceProxyHolder.get(origin);
        Object[] args = invocation.getArguments();
        return declared.invoke(proxy, args);
    }

    @Override
    public Class<?>[] getInterfaces() {
        return attachedInterfaces;
    }

    boolean inExpectedContext() {
        if (RootContext.requireGlobalLock()) {
            return true;
        }
        if (!RootContext.inGlobalTransaction()) {
            return false;
        }
        return branchType == RootContext.getBranchType();
    }
}
