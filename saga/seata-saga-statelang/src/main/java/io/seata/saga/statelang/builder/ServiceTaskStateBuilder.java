package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.builder.prop.ServiceTaskPropertyBuilder;
import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.builder.prop.TaskPropertyBuilder;
import io.seata.saga.statelang.domain.ServiceTaskState;

/***
 * Service task state builder for {@link ServiceTaskState}.
 *
 * @author ptyin
 */
public interface ServiceTaskStateBuilder extends
        StateBuilder<ServiceTaskState>,
        BasicPropertyBuilder<ServiceTaskStateBuilder>,
        TaskPropertyBuilder<ServiceTaskStateBuilder>,
        ServiceTaskPropertyBuilder<ServiceTaskStateBuilder> {

}
