package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.ServiceTaskState;

/***
 *
 * @author ptyin
 */
public interface ServiceTaskStateBuilder extends StateBuilder<ServiceTaskStateBuilder, ServiceTaskState>,
        TaskStateBuilder<ServiceTaskStateBuilder> {

    /**
     * Configure service method.
     *
     * @param serviceName name of service bean
     * @return builder for chaining
     */
    ServiceTaskStateBuilder withServiceName(String serviceName);


    /**
     * Configure service method.
     *
     * @param serviceMethod method of service bean
     * @return builder for chaining
     */
    ServiceTaskStateBuilder withServiceMethod(String serviceMethod);
}
