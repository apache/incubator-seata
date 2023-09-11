package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.builder.prop.ChoicePropertyBuilder;
import io.seata.saga.statelang.builder.prop.BasicPropertyBuilder;
import io.seata.saga.statelang.domain.ChoiceState;

/***
 * Choice state builder for {@link ChoiceState}
 *
 * @author ptyin
 */
public interface ChoiceStateBuilder extends
        StateBuilder<ChoiceState>,
        BasicPropertyBuilder<ChoiceStateBuilder>,
        ChoicePropertyBuilder<ChoiceStateBuilder> {

}
