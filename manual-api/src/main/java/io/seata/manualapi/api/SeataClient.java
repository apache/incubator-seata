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
