package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.builder.prop.ServiceTaskPropertyBuilder;
import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.builder.prop.TaskPropertyBuilder;
import io.seata.saga.statelang.domain.CompensateSubStateMachineState;

/***
 * Compensate SubStateMachine state builder for {@link CompensateSubStateMachineState}.
 *
 * @author ptyin
 */
public interface CompensateSubStateMachineStateBuilder extends
        StateBuilder<CompensateSubStateMachineState>,
        BasicPropertyBuilder<CompensateSubStateMachineStateBuilder>,
        TaskPropertyBuilder<CompensateSubStateMachineStateBuilder>,
        ServiceTaskPropertyBuilder<CompensateSubStateMachineStateBuilder> {

}
