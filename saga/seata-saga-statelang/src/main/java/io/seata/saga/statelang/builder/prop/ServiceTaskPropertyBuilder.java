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

package io.seata.saga.statelang.builder.prop;

import java.util.Collection;

/**
 * Service task property builder.
 *
 * @param <P> property builder type
 * @author ptyin
 */
public interface ServiceTaskPropertyBuilder<P extends ServiceTaskPropertyBuilder<P>> {

    /**
     * Configure service type such as SpringBean, SOFA RPC, default is StringBean.
     *
     * @param serviceType type of service
     * @return builder for chaining
     */
    P withServiceType(String serviceType);

    /**
     * Configure service method.
     *
     * @param serviceName name of service bean
     * @return builder for chaining
     */
    P withServiceName(String serviceName);

    /**
     * Configure service method.
     *
     * @param serviceMethod method of service bean
     * @return builder for chaining
     */
    P withServiceMethod(String serviceMethod);

    /**
     * Configure parameter types
     *
     * @param parameterTypes parameter types
     * @return builder for chaining
     */
    P withParameterTypes(Collection<String> parameterTypes);

    /**
     * Configure synchronization mode
     *
     * @param async async or not
     * @return builder for chaining
     */
    P withAsync(boolean async);
}
