package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.builder.prop.ScriptTaskPropertyBuilder;
import io.seata.saga.statelang.builder.prop.TaskPropertyBuilder;
import io.seata.saga.statelang.domain.ScriptTaskState;

/***
 * Script task state builder for {@link ScriptTaskState}
 *
 * @author ptyin
 */
public interface ScriptTaskStateBuilder extends
        StateBuilder<ScriptTaskState>,
        BasicPropertyBuilder<ScriptTaskStateBuilder>,
        TaskPropertyBuilder<ScriptTaskStateBuilder>,
        ScriptTaskPropertyBuilder<ScriptTaskStateBuilder> {

}
